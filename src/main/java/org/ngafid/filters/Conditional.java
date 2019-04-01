package org.ngafid.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;


public class Conditional {
    private static final Logger LOG = Logger.getLogger(Conditional.class.getName());

    private String type = null;

    private String parameterName = null;
    private double parameterValue = Double.NaN;

    //condition will either be <=, <, >, >= if this conditional's type is RULE
    //or AND or OR if it's type is GROUP
    private String condition = null;

    private double value;

    private ArrayList<Conditional> children = new ArrayList<Conditional>();

    /**
     * Creates a conditional from a filter
     *
     * @param inputs a list of inputs for the rule, e.g., "Pitch" "&gt;" "15.33"
     */
    public Conditional(Filter filter) {
        if (filter.type.equals("RULE")) {
            this.type = "RULE";
            this.parameterName = filter.inputs.get(0);
            this.condition = filter.inputs.get(1);
            this.value = Double.parseDouble(filter.inputs.get(2));

        } else if (filter.type.equals("GROUP")) {
            this.type = "GROUP";
            this.condition = filter.condition;

            for (Filter child : filter.filters) {
                children.add(new Conditional(child));
            }

        } else {
            LOG.severe("Could not create a conditional from a fitler with unknown rule type: '" + filter.type + "'");
            System.exit(1);
        }
    }

    /**
     * Sets the value for the parameter in this conditional and all of its children
     *
     * @param parameterName the name of the parameter to be set
     * @param parameterValue the value of the parameter being set
     */
    public void set(String parameterName, double parameterValue) {
        if (type.equals("RULE")) {
            if (this.parameterName.equals(parameterName)) {
                this.parameterValue = parameterValue;
            }
        } else if (type.equals("GROUP")) {
            for (Conditional child : children) {
                child.set(parameterName, parameterValue);
            }
        } else {
            LOG.severe("Could not set a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }
    }

    /**
     * Sets the value for the parameter in this conditional and all of its children
     *
     * @param parameterName the name of the parameter to be set
     * @param parameterValue the value of the parameter being set
     */
    public void set(String parameterName, Pair<Double,Double> minMax) {
        if (type.equals("RULE")) {
            if (this.parameterName.equals(parameterName)) {
                if (condition.equals("<=") || condition.equals("<")) {
                    this.parameterValue = minMax.first(); //set to min
                } else {
                    this.parameterValue = minMax.second(); //set to max
                } 
            }
        } else if (type.equals("GROUP")) {
            for (Conditional child : children) {
                child.set(parameterName, minMax);
            }
        } else {
            LOG.severe("Could not set a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }
    }


    /**
     *  Resets all the parameter values to NaN (unset).
     */
    public void reset() {
    }

    /**
     * Assuming all the parameters have been set this will evaluate the statement.
     *
     * @return if the statement evalutes to true or not
     */
    public boolean evaluate() {
        if (type.equals("RULE")) {
            if (Double.isNaN(parameterValue)) {
                //don't trigger exceedences on NaNs
                return false;
            }

            switch (condition) {
                case "<=" : return parameterValue <= value;
                case "<" : return parameterValue < value;
                case ">" : return parameterValue > value;
                case ">=" : return parameterValue >= value;

                default:
                    LOG.severe("Could not set a conditional with an unknown rule type: '" + this.type + "'");
                    System.exit(1);
                    return false;
            }

        } else if (type.equals("GROUP")) {
            if (condition.equals("AND")) {
                //if any child evaluates to false we can return
                //false early without evaluating the rest of them
                for (Conditional child : children) {
                    if (!child.evaluate()) return false;
                }
                return true;

            } else if (condition.equals("OR")) {
                //if any child evaluates to true we can return
                //true early without evaluating the rest of them
                for (Conditional child : children) {
                    if (child.evaluate()) return true;
                }
                return false;

            } else {
                LOG.severe("Could not evaluate a conditional on a group with an unknown condition type: '" + this.condition + "'");
                System.exit(1);
            }

        } else {
            LOG.severe("Could not evaluate a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }

        return false;
    }

    /**
     * Recursively returns a string representation of this filter and all it's children
     *
     * @return A string represntation of this filter
     */
    public String toString() {
        if (type.equals("RULE")) {
            String string = parameterName + "(" + parameterValue + ") " + condition + " " + value;

            return "(" + string + ")";

        } else if (type.equals("GROUP")) {
            String string = "";
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) string += " " + condition + " ";
                string += children.get(i).toString();
            }

            return "(" + string + ")";

        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
        return "";
    }
}
