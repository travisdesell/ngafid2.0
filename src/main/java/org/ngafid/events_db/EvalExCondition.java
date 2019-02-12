package org.ngafid.events_db;

import com.udojava.evalex.Expression;


import java.math.BigDecimal;

public class EvalExCondition {
    static Expression expression;

    public static void test(double pitch) {
        BigDecimal result = expression.with("pitch", Double.toString(pitch)).eval();
        System.out.println("result for pitch = " + pitch + ": " + result);
    }

    public static void main(String[] arguments) {
        expression = new Expression("pitch <= -30.0 || pitch >= 30.0");

        test(-35.0);
        test(-25.0);
        test(0.0);
        test(25.0);
        test(35.0);

        expression = new Expression("pitch <= -30.0 && roll >= 20.0");

        System.out.println("evaluating: '" + expression +"'");
        double pitch = -35.0;
        double roll = 25.0;

        System.out.println("pitch: " + pitch + ", roll: " + roll + ", result: " + expression.with("pitch", Double.toString(pitch)).with("roll", Double.toString(roll)).eval());

        System.out.println("evaluating: '" + expression +"'");
        pitch = 15.0;
        roll = 25.0;

        System.out.println("pitch: " + pitch + ", roll: " + roll + ", result: " + expression.with("pitch", Double.toString(pitch)).with("roll", Double.toString(roll)).eval());

        System.out.println("evaluating: '" + expression +"'");
        pitch = -35.0;
        roll = 20.0;

        System.out.println("pitch: " + pitch + ", roll: " + roll + ", result: " + expression.with("pitch", Double.toString(pitch)).with("roll", Double.toString(roll)).eval());
    }
}
