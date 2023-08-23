package org.ngafid.flights.process;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

/**
 * A dependency graph which represents the dependencies of ProcessSteps on one another.
 **/
public class DependencyGraph {
    private static final Logger LOG = Logger.getLogger(DependencyGraph.class.getName());

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

        void disableChildren() {
            if (enabled.get()) {
                enabled.set(false);
                if (step.isRequired()) {
                    String reason = step.explainApplicability();
                    LOG.severe("Required step " + step.getClass().getName() + " has been disabled for the following reason:\n    " + reason);
                    exceptions.add(new FatalFlightFileException(reason));
                }
                for (var child : requiredBy) child.disable();
            }
        }

        void disable() {
            if (enabled.get()) {
                enabled.set(false);
                if (step.isRequired()) {
                    LOG.severe("Required step " + step.toString() + " has been disabled.");
                    exceptions.add(
                        new FatalFlightFileException(
                            "Required step " + step.getClass().getName() 
                            + " has been disabled because a required parent step has been disabled"));
                }
                for (var child : requiredBy) child.disable();
            }
        }

        void compute() {
            try {
                if (step.applicable()) {
                    step.compute();
                } else {
                    disableChildren();
                }
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

            return null;
        }
    }

    /** 
     * Dummy step meant to act as a root node in DAG. This is done by adding all of the columns included in the file
     * as output columns, so all other steps will depend on this. 
     **/
    class DummyStep extends ProcessStep {
        Set<String> outputColumns = new HashSet<>();

        public DummyStep(FlightBuilder builder) {
            // We can pass in null rather than a connection object
            super(null, builder);
            outputColumns.addAll(doubleTS.keySet());
            outputColumns.addAll(stringTS.keySet());
        }

        public Set<String> getRequiredDoubleColumns() { return Collections.<String>emptySet(); }
        public Set<String> getRequiredStringColumns() { return Collections.<String>emptySet(); }
        public Set<String> getRequiredColumns() { return Collections.<String>emptySet(); }
        public Set<String> getOutputColumns() { return outputColumns; }

        public boolean airframeIsValid(String airframe) { return true; }


        // Left blank intentionally
        public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
            LOG.info("Computed dummy step!");
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
    FlightBuilder builder;
    
    public DependencyGraph(FlightBuilder builder, List<ProcessStep> steps) throws FlightProcessingException {
        /**
         *  Create nodes for each step and create a mapping from output column name
         *  to the node that outputs that column. This should be a unique mapping, as
         *  we don't want two steps generating the same output column.
         **/

        this.builder = builder;
        
        try {
            registerStep(new DummyStep(builder));
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
                var task = new DependencyNodeTask(node, tasks);
                initialTasks.add(task);
                tasks.put(node, task);
            }
        }
        
        scrutinize();

        var handles = initialTasks
            .stream()
            .map(x -> x.fork())
            .collect(Collectors.toList());
        handles.forEach(ForkJoinTask::join);
   
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

    public void scrutinize() {
        cycleCheck();
        requiredCheck();
    }

    // Ensure that there are no required steps that are children to optional steps,
    // since that wouldn't make sense.
    private void requiredCheck() {
        for (var node : nodes) {
            if (!node.step.isRequired())
                continue;

            for (var parent : node.requiredBy) {
                if (!parent.step.isRequired()) {
                    System.err.println("ERROR in your DependencyGraph! The optional step '" + parent.step + "' has a required dependent step '" + node.step + "'.");
                    System.exit(1);
                }
            }
        }
    }

    // Ensure there are no cycles!
    private void cycleCheck() {
        for (var src : nodes) {
            for (var node : nodes)
                node.mark = false;
            
            Queue<DependencyNode> q = new ArrayDeque<>();
            var dst = src;
            for (var child : src.requiredBy)
                q.add(child);

            while ((dst = q.poll()) != null) {
                if (dst == src) {
                    System.err.println("ERROR in your DependencyGraph! Cycle was detected from step '" + src + "' to step '" + dst + "'.");
                    System.exit(1);
                }

                dst.mark = true;
                for (var child : dst.requiredBy) {
                    if (!child.mark)
                        q.add(child);
                }
            }
        }
    }
}
