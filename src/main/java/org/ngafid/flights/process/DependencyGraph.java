package org.ngafid.flights.process;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

/**
 * A directed acyclic graph, where nodes are individual process steps and the edges are defined by column names that are
 * either input or output by that process step. A dummy node is created in the case of columns that are contained in the
 * data source.
 *
 * A basic understanding of how ForkJoin tasks work in Java is a prerequisite to understand this.
 *
 * @author Joshua Karns (josh@karns.dev)
 **/
public class DependencyGraph {
    private static final Logger LOG = Logger.getLogger(DependencyGraph.class.getName());

    final ConcurrentHashMap<DependencyNode, ForkJoinTask<Void>> taskMap;

    ForkJoinTask<Void> getTask(DependencyNode node) {
        return taskMap.computeIfAbsent(node, x -> x.fork());
    }

    /**
     * A single node in the DependencyGraph, correspnding to a single process step. Incoming and outgoing edges are
     * stored in the `requires` and `requiredBy` fields.
     */
    private class DependencyNode extends RecursiveTask<Void> {
        // Supresses a compiler error, not relevant or important since this will not be serialized
        private static final long serialVersionUID = 0;

        // The process step to execute.
        final ProcessStep step;

        // Used for cycle detection.
        boolean mark = false;

        // Whether this process step should be executed. This will be set to `false` if an ancestor which this node is
        // contingent on raises an exception.
        AtomicBoolean enabled = new AtomicBoolean(true);

        // Outgoing edges.
        final HashSet<DependencyNode> requiredBy = new HashSet<>(32);

        // Incoming edges.
        final HashSet<DependencyNode> requires = new HashSet<>(32);

        // A list of exceptions that could be created during the execution of this process step.
        ArrayList<Exception> exceptions = new ArrayList<>();

        public DependencyNode(ProcessStep step) {
            this.step = step;
        }

        /**
         * Disable this node and all descendent nodes.
         */
        void disableChildren() {
            if (enabled.get()) {
                enabled.set(false);

                String reason = step.explainApplicability();
                if (step.isRequired()) {
                    LOG.severe("Required step " + step.getClass().getName()
                            + " has been disabled for the following reason:\n    " + reason);
                    exceptions.add(new FatalFlightFileException(reason));
                } else {
                    LOG.finer("Optional step " + step.getClass().getName() + " has been disabled because:\n    "
                            + reason);
                }
                for (var child : requiredBy)
                    child.disableChildren();
            }
        }

        /**
         * Attempts to compute the process step in this node, if the step is applicable. If it is not applicable, child
         * process steps are disabled.
         *
         * Exceptions are caught and stored, and will cause descendent steps to be disabled.
         */
        public Void compute() {
            for (var requiredNode : requires) {
                getTask(requiredNode).join();
            }

            if (enabled.get()) {

                try {
                    if (step.applicable()) {
                        step.compute();
                    } else {
                        disableChildren();
                    }
                } catch (SQLException | MalformedFlightFileException | FatalFlightFileException e) {
                    LOG.warning(
                            "Encountered exception when calculating process step " + step.toString() + ": "
                                    + e.toString());
                    exceptions.add(e);
                    disableChildren();
                }
            }

            return null;
        }
    }

    /**
     * Dummy step meant to act as a root node in DAG. This is done by adding all of the columns included in the file as
     * output columns, so all other steps will depend on this.
     **/
    class DummyStep extends ProcessStep {
        Set<String> outputColumns = new HashSet<>();

        public DummyStep(FlightBuilder builder) {
            // We can pass in null rather than a connection object
            super(null, builder);
            outputColumns.addAll(builder.getDoubleTimeSeriesKeySet());
            outputColumns.addAll(builder.getStringTimeSeriesKeySet());
        }

        public Set<String> getRequiredDoubleColumns() {
            return Collections.<String>emptySet();
        }

        public Set<String> getRequiredStringColumns() {
            return Collections.<String>emptySet();
        }

        public Set<String> getRequiredColumns() {
            return Collections.<String>emptySet();
        }

        public Set<String> getOutputColumns() {
            return outputColumns;
        }

        public boolean airframeIsValid(String airframe) {
            return true;
        }

        // Left blank intentionally
        public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
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

    /**
     * Add a process step node to the set of nodes and verify there are no other nodes that have the same output
     * column(s).
     */
    private DependencyNode registerStep(ProcessStep step) throws FatalFlightFileException {
        DependencyNode node = new DependencyNode(step);
        nodes.add(node);

        for (String outputColumn : step.getOutputColumns()) {
            DependencyNode other = null;
            if ((other = columnToSource.put(outputColumn, node)) != null)
                nodeConflictError(step, other.step);
        }

        return node;
    }

    /**
     * Create the edges. An edge exists from step X to step Y if step X has an output column that step Y relies upon.
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

    /**
     * Create nodes for each step and create a mapping from output column name
     * to the node that outputs that column. This should be a unique mapping, as
     * we don't want two steps generating the same output column.
     **/
    public DependencyGraph(FlightBuilder builder, List<ProcessStep> steps) throws FlightProcessingException {
        this.taskMap = new ConcurrentHashMap<>(steps.size() * 2);
        this.builder = builder;

        try {
            registerStep(new DummyStep(builder));
            for (var step : steps)
                registerStep(step);
            for (var node : nodes)
                createEdges(node);
        } catch (FatalFlightFileException e) {
            throw new FlightProcessingException(e);
        }
    }

    /**
     * Compute all process steps, possibly concurrently depending on the executor that this method is invoked in. The
     * order the process steps are executed in will not always be the same, but it will always be in a manner which
     * obeys column requirements.
     *
     * If this method is invoked in the context of a ForkJoinPool or some other executor / threadpool, it will be
     * executed concurrently. Otherwise, it will be executed it will be computed sequentially.
     */
    public void compute() throws FlightProcessingException {
        // Start with root nodes only
        ConcurrentHashMap<DependencyNode, ForkJoinTask<Void>> tasks = new ConcurrentHashMap<>();
        ArrayList<ForkJoinTask<Void>> initialTasks = new ArrayList<>();

        for (var node : nodes) {
            if (node.requiredBy.size() == 0) {
                initialTasks.add(node);
                tasks.put(node, node);
            }
        }

        scrutinize();

        // We need to fork all of the root tasks before we proceed to join with them!
        var handles = initialTasks
                .stream()
                .map(x -> x.fork())
                .collect(Collectors.toList());

        // All tasks have been created: join them.
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
                    System.err.println("ERROR in your DependencyGraph! The optional step '" + parent.step
                            + "' has a required dependent step '" + node.step + "'.");
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
                    System.err.println("ERROR in your DependencyGraph! Cycle was detected from step '" + src
                            + "' to step '" + dst + "'.");
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