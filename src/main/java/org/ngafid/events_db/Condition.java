
package org.ngafid.events_db;

import java.util.Scanner;

// current < minValue || current > maxValue
//current1 || current2 

class Condition {
    char operator;
    double min;
    double max;

    Condition(String conditionString) {
        //use scanner to process conditionString and set up 
        //this object
    }

    public static void main(String[] args) {

        char operator;
        Double current1, current2, result;
        //String rull, result;

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter operator (either || or &&): ");

        operator = scanner.next().charAt(0);
        System.out.print("Enter current1, rull and current2 respectively: ");
        current1 = scanner.nextDouble();
        //rull = scanner.nextString();
        current2 = scanner.nextDouble();

        switch (operator) {
            //  case '||':
            //    result = current1 +rull+ current2;
            //  System.out.print(current1 + "number1" + rull "||"+ current2 + " number2 " + result);
            //    break;

            case '-':
                result = current1 - current2;
                System.out.print(current1 + "-" + current2 + " = " + result);
                break;

            case '*':
                result = current1 * current2;
                System.out.print(current1 + "*" + current2 + " = " + result);
                break;

            case '/':
                result = current1 / current2;
                System.out.print(current1 + "/" + current2 + " = " + result);
                break;

            default:
                System.out.println("Invalid operator!");
                break;
        }
    }
}
