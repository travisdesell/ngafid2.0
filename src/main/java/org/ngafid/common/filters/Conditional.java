package org.ngafid.common.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


public class Conditional {
    private static final Logger LOG = Logger.getLogger(Conditional.class.getName());

    private static final Set<String> VALID_RULE_CONDITIONS = Set.of("<", ">", ">=", "<=");
    private static final Set<String> VALID_GROUP_CONDITIONS = Set.of("AND", "OR");


    private String type = null;

    private String parameterName = null;
    private double parameterValue = Double.NaN;

    //condition will either be <=, <, >, >= if this conditional's type is RULE
    //or AND or OR if it's type is GROUP
    private String condition = null;

    private double value;

    private final ArrayList<Conditional> children = new ArrayList<Conditional>();

    /**
     * Creates a conditional from a filter
     *
     * @param filter the filter to create the conditional from
     */
    public Conditional(Filter filter) {
        if (filter.type.equals("RULE")) {
            this.type = "RULE";
            this.parameterName = filter.inputs.get(0);
            this.condition = filter.inputs.get(1);
            if (!VALID_RULE_CONDITIONS.contains(this.condition)) {
                LOG.severe("Could not set a invalid condition for a rule: '" + this.condition + "'");
                System.exit(1);
            }
            this.value = Double.parseDouble(filter.inputs.get(2));

        } else if (filter.type.equals("GROUP")) {
            this.type = "GROUP";
            this.condition = filter.condition;

            if (!VALID_GROUP_CONDITIONS.contains(this.condition)) {
                LOG.severe("Could not set a invalid condition for a group: ''" + this.type + "'");
                System.exit(1);
            }

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
     * @param paramNameToBeSet the name of the parameter to be set
     * @param newParamValue    the value of the parameter being set
     */
    public void set(String paramNameToBeSet, double newParamValue) {
        if (type.equals("RULE")) {
            if (this.parameterName.equals(paramNameToBeSet)) {
                this.parameterValue = newParamValue;
            }
        } else if (type.equals("GROUP")) {
            for (Conditional child : children) {
                child.set(paramNameToBeSet, newParamValue);
            }
        } else {
            LOG.severe("Could not set a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }
    }

    /**
     * Sets the value for the parameter in this conditional and all of its children
     *
     * @param paramNameToBeSet the name of the parameter to be set
     * @param minMax           the min and max values of the parameter being set
     */
    public void set(String paramNameToBeSet, Pair<Double, Double> minMax) {
        if (type.equals("RULE")) {
            if (this.parameterName.equals(paramNameToBeSet)) {
                if (condition.equals("<=") || condition.equals("<")) {
                    this.parameterValue = minMax.first(); //set to min
                } else {
                    this.parameterValue = minMax.second(); //set to max
                }
            }
        } else if (type.equals("GROUP")) {
            for (Conditional child : children) {
                child.set(paramNameToBeSet, minMax);
            }
        } else {
            LOG.severe("Could not set a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }
    }


    /**
     * Resets all the parameter values to NaN (unset).
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
                case "<=":
                    return parameterValue <= value;
                case "<":
                    return parameterValue < value;
                case ">":
                    return parameterValue > value;
                case ">=":
                    return parameterValue >= value;

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
                LOG.severe("Could not evaluate a conditional on a group " +
                        "with an unknown condition type: '" + this.condition + "'");
                System.exit(1);
            }

        } else {
            LOG.severe("Could not evaluate a conditional with an unknown rule type: '" + this.type + "'");
            System.exit(1);
        }

        return false;
    }

    /**
     * Generate java source code from this conditional.
     *
     * @return the java source code
     */
    public String codeGen() {
        HashSet<String> parameters = new HashSet<>();
        StringBuilder conditionSB = new StringBuilder();
        this.codeGen(conditionSB, parameters);
        String conditionStr = conditionSB.toString();

        StringBuilder classSB = new StringBuilder();

        classSB.append("import java.util.List;\n" + "import java.util.HashMap;\n\n" + "public class " +
                "CompiledCondition" + this.hashCode() + " {\n\n" + "    private int length = -1;\n");

        for (String parameter : parameters) {
            classSB.append("    private double[] " + parameter + " = null;\n");
        }
        classSB.append("\n");
        classSB.append("    public CompiledCondition(int length, HashMap<String, double[]> parameterMap) {\n" + "    " +
                "    this.length = length;");
        for (String parameter : parameters) {
            classSB.append("        this." + parameter + "Series = parameterMap.get(\"" + parameter + "\");\n");
        }
        classSB.append("}\n\n");

        classSB.append("    public boolean evaluate(int timeStep) {\n");

        for (String parameter : parameters) {
            classSB.append("        double " + parameter + " = this." + parameter + "Series[timeStep];\n");
        }

        return conditionStr;
    }

    private void codeGen(StringBuilder sb, HashSet<String> parameters) {
        if (type.equals("RULE")) {
            parameters.add(parameterName);

            sb.append("(");
            // This is just an inlined NaN test
            // https://stackoverflow.com/questions/18442503/java-isnan-how-it-works
            sb.append("!(" + parameterName + " != " + parameterName + ")");
            sb.append(" && ");
            // Hex string so we don't lose precision by rouding. toString may round a float to make it pretty
            sb.append(parameterName + " " + condition + " " + Double.toHexString(this.value));
            sb.append(")");
        } else if (type.equals("GROUP")) {
            sb.append("(");
            if (condition.equals("AND")) {
                for (Conditional child : children) {
                    child.codeGen(sb, parameters);
                    sb.append(" && ");
                }
                sb.append("true");
            } else if (condition.equals("OR")) {
                for (Conditional child : children) {
                    child.codeGen(sb, parameters);
                    sb.append(" || ");
                }
                sb.append("false");
            }
            sb.append(")");
        }
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
