package org.ngafid.flights.process;

import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;
import java.util.Queue;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import org.ngafid.flights.Flight;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

import org.ngafid.flights.process.ProcessStep;

/**
 * A dependency graph which represents the dependencies of ProcessSteps on one another.
 **/
public class DependencyGraph {
    private static final Logger LOG = Logger.getLogger(DependencyGraph.class.getName());

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors();
    
    class DependencyNode {
        final ProcessStep step;

        // Used for cycle detection.
        boolean mark = false;
        AtomicBoolean enabled = new AtomicBoolean(true);

        final HashSet<DependencyNode> requiredBy = new HashSet<>(32);
        final HashSet<DependencyNode> requires = new HashSet<>(32);

        ArrayList<Exception> exceptions = new ArrayList<>();

        public DependencyNode(ProcessStep step) {
            this.step = step;
        }

        void disable() {
            enabled.set(false);
            if (step.isRequired()) {
                LOG.severe("Required step " + step.toString() + " has been disabled.");
                exceptions.add(new FatalFlightFileException("Required step " + step.toString() + " has been disabled."));
            }
            for (var child : requiredBy)
                child.disable();
        }

        void compute() {
            try {
                
                if (step.applicable())
                    step.compute();
                else
                    disable();

            } catch (SQLException | MalformedFlightFileException | FatalFlightFileException e) {
                LOG.warning("Encountered exception when calculating process step " + step.toString() + ": " + e.toString());
                exceptions.add(e);
                disable();
            }
        }
    }

    class DependencyNodeTask extends RecursiveTask<Void> {
        private static final long serialVersionUID = 0; 

        // This is used to avoid creating duplicate tasks.
        // This isn't a problem w/ a tree-like problem, but ours is a DAG.
        final ConcurrentHashMap<DependencyNode, ForkJoinTask<Void>> taskMap;
        final DependencyNode node;

        public DependencyNodeTask(DependencyNode node, ConcurrentHashMap<DependencyNode, ForkJoinTask<Void>> taskMap) {
            this.taskMap = taskMap;
            this.node = node;
        }

        ForkJoinTask<Void> getTask(DependencyNode node) {
            return taskMap.computeIfAbsent(node, x -> new DependencyNodeTask(x, taskMap).fork());
        }

        public Void compute() {
            for (var requiredNode : node.requires) {
                getTask(requiredNode).join();
            }
            
            if (node.enabled.get())
                node.compute();
            else {} // TODO:  Add some sort of exception here. We don't want to just silently
                    //        let the processing pipeline fail somewhere

            return null;
        }
    }

    private void nodeConflictError(ProcessStep first, ProcessStep second) throws FatalFlightFileException {
        throw new FatalFlightFileException( 
           "ERROR when building dependency graph! "
           + "Two ProcessSteps are indicated as having the same output column. "
           + "While it is possible for two ProcessSteps to have the same output column(s), " 
           + "their use should be mutually exclusive from one another. "
           + "\nDEBUG INFO:\n node 0: " + first.toString() + "\n node 1: " + second.toString());
        
    }

    private DependencyNode registerStep(ProcessStep step) throws FatalFlightFileException {
        DependencyNode node = new DependencyNode(step);
        nodes.add(node);
        
        for (String outputColumn : step.getOutputColumns()) {
            DependencyNode other = null;
            if ((other = columnToSource.put(outputColumn, node)) != null) nodeConflictError(step, other.step);
        }

        return node;
    }
    
    /**
     * Create the edges. An edge exists from step X to step Y if step X has an output column
     * that step Y relies upon.
     **/
    private void createEdges(DependencyNode node) throws FatalFlightFileException {
        for (String column : node.step.getRequiredColumns()) {
            DependencyNode sourceNode = columnToSource.get(column);
            if (sourceNode != null) {
                sourceNode.requiredBy.add(node);
                node.requires.add(sourceNode);
            }
        }
    }

    // Maps column name to the node where that column is computed
    HashMap<String, DependencyNode> columnToSource = new HashMap<>(64);
    HashSet<DependencyNode> nodes = new HashSet<>(64);
    DependencyNode rootNode;
    FlightBuilder builder;
    
    public DependencyGraph(FlightBuilder builder, ArrayList<ProcessStep> steps) throws FlightProcessingException {
        /**
         *  Create nodes for each step and create a mapping from output column name
         *  to the node that outputs that column. This should be a unique mapping, as
         *  we don't want two steps generating the same output column.
         **/

        this.builder = builder;
        
        try {
            rootNode = registerStep(new DummyProcessStep(flight));
            for (var step : steps) registerStep(step);
            for (var node : nodes) createEdges(node);
        } catch (FatalFlightFileException e) {
            throw new FlightProcessingException(e);
        }
    }

    // Modifies the flight object in place.
    public void compute() throws FlightProcessingException {
        // Start with all of the leaf nodes.
        ConcurrentHashMap<DependencyNode, ForkJoinTask<Void>> tasks = new ConcurrentHashMap<>();
        ArrayList<ForkJoinTask<Void>> initialTasks = new ArrayList<>();
        for (var node : nodes) {
            if (node.requiredBy.size() == 0) {
                var task = new DependencyNodeTask(rootNode, tasks);
                initialTasks.add(task);
                tasks.put(node, task);
            }
        }

        ForkJoinPool ex = new ForkJoinPool();

        try {
            ex.invoke(new RecursiveTask<Void>() {
                public Void compute() {
                    initialTasks
                        .stream()
                        .map(x -> x.fork())
                        .map(x -> x.join())
                        .count();
                    return null;
                }
            });
        } finally {
            ex.shutdown();
        }
   
        ArrayList<Exception> fatalExceptions = new ArrayList<>();
        for (var node : nodes) {
            for (var e : node.exceptions) {
                if (e instanceof MalformedFlightFileException me) {
                    builder.exceptions.add(me);
                } else if (e instanceof FatalFlightFileException fe) {
                    fatalExceptions.add(fe);
                } else if (e instanceof SQLException se) {
                    fatalExceptions.add(se);
                } else {
                    LOG.severe(
                        "Encountered exception of unknown type when executing dependency graph. "
                        + "\"" + e.getMessage() + "\"" + "\n."
                        + "This should not be possible - if this seems plausible you should add a handler for this "
                        + "type of exception in DependencyGraph::compute.");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        if (fatalExceptions.size() != 0)
            throw new FlightProcessingException(fatalExceptions);
    }

    public void cycleCheck() throws FlightProcessingException {
        // TODO: Cycle check
    }
}
