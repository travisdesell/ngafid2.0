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

        int pitchColumn = 13;
        double maxPitch = 10;

        int lineNumber = 2;

        String line;
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
                //System.out.print(pitch);

                //Excessive Pitch is defined as pitch in excess of 30 degrees.
                if (pitch > maxPitch || pitch < -maxPitch) {
                    System.out.print(pitch);
                    System.out.print(" -- PITCH EXCEEDENCE ON LINE " + lineNumber);
                    System.out.println();
                }


            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                System.exit(1);
            }

            lineNumber++;
		}


	}

}
