/* Persist class

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that redistribution of source code include
the following disclaimer in the documentation and/or other materials provided
with the distribution.

THIS SOFTWARE IS PROVIDED BY ITS CREATOR "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE CREATOR OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.ngafid.flights.dji;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class DATPersist {

    static File persistenceFile = null;

    public static boolean showUnits = false;

    public static boolean EXPERIMENTAL_FIELDS = false;

    public static boolean autoTransDJIAFiles = true;

    public static boolean loadLastOnStartup = false;

    public static boolean showNewVerAvail = true;

    public static boolean checkUpdts = true;

    public static String inputFileName = "";

    public static String outputDirName = "";

    public static boolean EXPERIMENTAL_DEV = false;

    public static boolean motorPowerCalcs = false;

    public static boolean inertialOnlyCalcs = false;

    public static boolean magCalcs = false;

    public static boolean airComp = false;

    public static boolean invalidStructOK = false;

    static public ParsingMode parsingMode = ParsingMode.JUST_ENGINEERED;

    public static Dimension datConSize = new Dimension(900, 950);

    public static int csvSampleRate = 30;

    public static boolean logPanelEFB = false;

    public static boolean logPanelCFB = false;

    public static boolean logPanelRDFB = false;

    public static boolean smartTimeAxis = true;

    public enum ParsingMode {
        JUST_DAT, JUST_ENGINEERED, DAT_THEN_ENGINEERED, ENGINEERED_THEN_DAT, ENGINEERED_AND_DAT
    }

    ;

    public DATPersist() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && userHome.length() > 0) {
            persistenceFile = new File(userHome + "/.datCon");
        }

        load();
    }

    public static void save() {
        try {
            PrintStream ps = new PrintStream(persistenceFile);

            if (outputDirName != null) {
                ps.println("outputDir:" + outputDirName);
            }
            if (inputFileName != null) {
                ps.println("inputFile:" + inputFileName);
            }
            if (checkUpdts) {
                ps.println("checkUpdates:true");
            } else {
                ps.println("checkUpdates:false");
            }
            if (showNewVerAvail) {
                ps.println("showNewVerAvail:true");
            } else {
                ps.println("showNewVerAvail:false");
            }
            if (loadLastOnStartup) {
                ps.println("loadLastOnStartup:true");
            } else {
                ps.println("loadLastOnStartup:false");
            }
            if (autoTransDJIAFiles) {
                ps.println("autoExtractDJIAFiles:true");
            } else {
                ps.println("autoExtractDJIAFiles:false");
            }
            if (EXPERIMENTAL_FIELDS) {
                ps.println("experimentalFields:true");
            } else {
                ps.println("experimentalFields:false");
            }
            if (showUnits) {
                ps.println("showUnits:true");
            } else {
                ps.println("showUnits:false");
            }
            if (motorPowerCalcs) {
                ps.println("motorPowerCalcs:true");
            } else {
                ps.println("motorPowerCalcs:false");
            }
            if (magCalcs) {
                ps.println("magCalcs:true");
            } else {
                ps.println("magCalcs:false");
            }
            if (airComp) {
                ps.println("airComp:true");
            } else {
                ps.println("airComp:false");
            }
            if (inertialOnlyCalcs) {
                ps.println("inertialOnlyCalcs:true");
            } else {
                ps.println("inertialOnlyCalcs:false");
            }

            switch (parsingMode) {
                case DAT_THEN_ENGINEERED -> ps.println("parsingMode:DAT_THEN_DEFINED");
                case ENGINEERED_THEN_DAT -> ps.println("parsingMode:DEFINED_THEN_DAT");
                case ENGINEERED_AND_DAT -> ps.println("parsingMode:ENGINEERED_AND_DAT");
                case JUST_DAT -> ps.println("parsingMode:JUST_DAT");
                case JUST_ENGINEERED -> ps.println("parsingMode:JUST_DEFINED");
                default -> {}
            }

            if (invalidStructOK) {
                ps.println("invalidStructOK:true");
            } else {
                ps.println("invalidStructOK:false");
            }
            if (datConSize != null) {
                ps.println(String.format("datConSize:%d,%d",
                        (long) datConSize.getWidth(),
                        (long) datConSize.getHeight()));
            }
            ps.println("csvSampleRate:" + csvSampleRate);

            ps.println("logPanelEFB:" + logPanelEFB);
            ps.println("logPanelCFB:" + logPanelCFB);
            ps.println("logPanelRDFB:" + logPanelRDFB);
            ps.println("smartTimeAxis:" + smartTimeAxis);

            ps.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    public static boolean load() {
        boolean success = true;
        FileReader input = null;
        String userHome = System.getProperty("user.home");
        if (userHome != null && userHome.length() > 0) {
            persistenceFile = new File(userHome + "/.datCon");
        }

        try {
            input = new FileReader(persistenceFile);
        } catch (FileNotFoundException e) {
            // nothing to do
        }
        if (input != null) {
            BufferedReader br = new BufferedReader(input);
            String line = null;
            int index = 0;
            try {
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("outputDir:") == 0) {
                        index = line.indexOf(":") + 1;
                        outputDirName = line.substring(index);
                    }
                    if (line.indexOf("inputFile:") == 0) {
                        index = line.indexOf(":") + 1;
                        String inputF = line.substring(index);
                        inputFileName = inputF;
                    }
                    if (line.indexOf("checkUpdates:") == 0) {
                        checkUpdts = parseBoolean(line);

                    }
                    if (line.indexOf("showNewVerAvail:") == 0) {
                        showNewVerAvail = parseBoolean(line);
                    }
                    if (line.indexOf("loadLastOnStartup:") == 0) {
                        loadLastOnStartup = parseBoolean(line);

                    }
                    if (line.indexOf("autoExtractDJIAFiles:") == 0) {
                        autoTransDJIAFiles = parseBoolean(line);

                    }
                    if (line.indexOf("experimentalFields:") == 0) {
                        EXPERIMENTAL_FIELDS = parseBoolean(line);

                    }
                    if (line.indexOf("showUnits:") == 0) {
                        showUnits = parseBoolean(line);

                    }
                    if (line.indexOf("motorPowerCalcs:") == 0) {
                        motorPowerCalcs = parseBoolean(line);

                    }
                    if (line.indexOf("magCalcs:") == 0) {
                        magCalcs = parseBoolean(line);
                    }

                    if (line.indexOf("inertialOnlyCalcs:") == 0) {
                        inertialOnlyCalcs = parseBoolean(line);
                    }
                    if (line.indexOf("airComp:") == 0) {
                        airComp = parseBoolean(line);

                    }
                    if (line.indexOf("parsingMode:") == 0) {
                        index = line.indexOf(":") + 1;
                        String mode = line.substring(index);
                        if (mode.equalsIgnoreCase("DAT_THEN_DEFINED")) {
                            parsingMode = ParsingMode.DAT_THEN_ENGINEERED;
                        } else if (mode.equalsIgnoreCase("DEFINED_THEN_DAT")) {
                            parsingMode = ParsingMode.ENGINEERED_THEN_DAT;
                        } else if (mode
                                .equalsIgnoreCase("ENGINEERED_AND_DAT")) {
                            parsingMode = ParsingMode.ENGINEERED_AND_DAT;
                        } else if (mode.equalsIgnoreCase("JUST_DAT")) {
                            parsingMode = ParsingMode.JUST_DAT;
                        } else if (mode.equalsIgnoreCase("JUST_DEFINED")) {
                            parsingMode = ParsingMode.JUST_ENGINEERED;
                        }
                    }
                    if (line.indexOf("invalidStructOK:") == 0) {
                        invalidStructOK = parseBoolean(line);

                    }
                    if (line.indexOf("datConSize:") == 0) {
                        index = line.indexOf(":") + 1;
                        String size = line.substring(index);
                        String[] tokens = size.split(",");
                        if (tokens.length == 2) {
                            datConSize = new Dimension(
                                    Integer.parseInt(tokens[0]),
                                    Integer.parseInt(tokens[1]));
                        }
                    }
                    if (line.indexOf("csvSampleRate:") == 0) {
                        index = line.indexOf(":") + 1;
                        String rate = line.substring(index);
                        csvSampleRate = Integer.parseInt(rate);
                    }
                    if (line.indexOf("logPanelEFB:") == 0) {
                        logPanelEFB = parseBoolean(line);
                    }
                    if (line.indexOf("logPanelCFB:") == 0) {
                        logPanelCFB = parseBoolean(line);
                    }
                    if (line.indexOf("logPanelRDFB:") == 0) {
                        logPanelRDFB = parseBoolean(line);
                    }
                    if (line.indexOf("smartTimeAxis:") == 0) {
                        smartTimeAxis = parseBoolean(line);
                    }
                }
                input.close();
            } catch (IOException e) {
                System.out.println("IOException Occured in DATPersist");
            }
        }

        return success;
    }

    private static boolean parseBoolean(String line) {
        int index = line.indexOf(":") + 1;
        String state = line.substring(index);
        return (state.equalsIgnoreCase("true"));
    }
}
