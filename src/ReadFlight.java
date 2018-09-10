// Java Program to illustrate reading from FileReader
// using BufferedReader
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ReadFlight {

    public static void main(String[] arguments) throws Exception {
        // We need to provide file path as the parameter:
        // double backquote is to avoid compiler interpret words
        // like \test as \t (ie. as a escape sequence)

        System.out.println("Command Line Arguments:");
        for (int i = 0; i < arguments.length; i++) {
            System.out.println("arguments[" + i + "]: '" + arguments[i] + "'");
        }

        if (arguments.length == 0) {
            System.err.println("Incorrect arguments, should take a flight file.");
            System.exit(1);
        }

        File file = new File(arguments[0]);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        //file information -- this was the first line
        String fileInformation = bufferedReader.readLine();
        String[] dataTypes = bufferedReader.readLine().split("\\,", -1);;
        String[] headers = bufferedReader.readLine().split("\\,", -1);;

        System.out.println("Headers:");
        for (int i = 0; i < headers.length; i++) {
            System.out.println("\theaders[" + i + "]: '" + headers[i].trim() + "' (" + dataTypes[i].trim() + ")");
        }

        int timeColumn = 1;
        int pitchColumn = 13;
        double maxPitch = 10;
        int exceedenceBuffer = 10; //10 lines (not seconds)

        int lineNumber = 2;


        String line;
        int exceedenceCount = 0;
        String exceedenceStartTime = "";
        int exceedenceStartLine = 0;
        String exceedenceEndTime = "";
        int exceedenceEndLine = 0;
        String previousTime = "";

        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);

            String[] values = line.split("\\,", -1);
            /*
               for (int i = 0; i < values.length; i++) {
               System.out.println("\tvalues[" + i + "]: '" + values[i].trim() + "'");
               }
             */

            try {
                double pitch = Double.parseDouble(values[pitchColumn]);
                System.out.println(lineNumber + " : " + values[timeColumn] + " : " + pitch);

                //Excessive Pitch is defined as pitch in excess of 30 degrees.
                if (pitch > maxPitch || pitch < -maxPitch) {
                    //If we get to this part of the if statement, then the current line's pitch value
                    //was less than -maxPitch or greater than maxPitch

                    if (exceedenceCount == 0) {
                        //If the exceedenceCount is zero, that means this was the start of a new exceedence

                        exceedenceStartTime = values[timeColumn];
                        exceedenceStartLine = lineNumber;
                        System.out.println("Exceedence started at: " + exceedenceStartTime);
                        exceedenceCount++;
                    } else {
                        //If the exceedenceCount is NOT zero, that means previous lines were also part
                        //of the exceedence

                        exceedenceCount++;
                    }

                    System.out.println("PITCH EXCEEDENCE ON LINE " + lineNumber + " AT TIME " + values[timeColumn]);

                    if (exceedenceCount == 0 || lineNumber - exceedenceEndLine < exceedenceBuffer){
                        exceedenceStartTimeUpdate=exceedenceStartTime;
                        exceeedenceStartLineUpdate=exceedenceStartLine;
                        System.out.println("Exceedenced merged started time at:" + exceedenceStartTimeUpdate);
                        exceedenceCount++;
                    }

                } else if (exceedenceCount > 0) {
                    //If we get to this part of the if statement, pitch was NOT greater than maxPitch or less than -maxPitch
                    //If the exceedenceCount is greater than 0, then previous lines were part of an exceedence

                    exceedenceEndTime = previousTime;
                    exceedenceEndLine = lineNumber - 1;
                    exceedenceCount = 0;

                    System.out.println("Exceedence ended at: " + exceedenceEndTime);
                    System.out.println("Exceedence ran from: " + exceedenceStartTime + " to " + exceedenceEndTime);
                }

                previousTime = values[timeColumn];

            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                System.exit(1);
            } catch (ArrayIndexOutOfBoundsException aoobe) {
                aoobe.printStackTrace();
                System.out.println("line was: " + lineNumber);
                System.out.println("values were:");
                for (int i = 0; i < values.length; i++) {
                    System.out.println("\t" + values[i]);
                }
                System.exit(1);
            }

            lineNumber++;
        }


    }

}
