package org.ngafid;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;

import org.ngafid.accounts.EmailType;

import java.util.logging.Logger;

public class UploadProcessedEmail {

    private static Logger LOG = Logger.getLogger(ProcessUpload.class.getName());

    private static enum FlightStatus {
        OK, ERROR, WARNING
    }

    private static class FlightInfo {
        /**
         * This is a helper class so we don't keep all loaded flights in memory.
         */

        int id;
        int length;
        String filename;

        FlightStatus status = FlightStatus.OK;

        TreeSet<String> errorMessages = new TreeSet<String>();
        TreeSet<String> warningMessages = new TreeSet<String>();

        TreeSet<String> exceedenceMessages = new TreeSet<String>();
        TreeSet<String> exceedenceErrorMessages = new TreeSet<String>();

        TreeSet<String> proximityMessages = new TreeSet<String>();
        TreeSet<String> proximityErrorMessages = new TreeSet<String>();

        TreeSet<String> ttfErrorMessages = new TreeSet<String>();

        public FlightInfo(String filename) {
            this.filename = filename;
        }

        public FlightInfo(String filename, int id, int length) {
            this.filename = filename;
            this.id = id;
            this.length = length;
        }

        public void setOK() {
            status = FlightStatus.OK;
        }

        public void setError(String message) {
            status = FlightStatus.ERROR;
            errorMessages.add(message);
        }

        public void setWarning(String message) {
            status = FlightStatus.WARNING;
            warningMessages.add(message);
        }

        public boolean wasOK() {
            return status == FlightStatus.OK;
        }

        public boolean wasWarning() {
            return status == FlightStatus.WARNING;
        }

        public boolean wasError() {
            return status == FlightStatus.ERROR;
        }

        public void addExceedence(String message) {
            exceedenceMessages.add(message);
        }

        public void addExceedenceError(String message) {
            exceedenceErrorMessages.add(message);
        }

        public void addProximity(String message) {
            proximityMessages.add(message);
        }

        public void addProximityError(String message) {
            proximityErrorMessages.add(message);
        }

        public void addTTFError(String message) {
            ttfErrorMessages.add(message);
        }

        public void getDetails(StringBuilder body) {
            if (status == FlightStatus.ERROR) {
                body.append("&emsp; <a href='http://ngafid.org/protected/flight?flight_id=" + id + "'>flight " + id + "</a> imported with errors:");
                body.append("<br>");
                if (errorMessages.size() > 1) {
                    body.append("<br>");
                    for (String message : errorMessages) {
                        body.append("&emsp; &emsp; " + message + "<br>");
                    }
                } else {
                    for (String message : errorMessages) {
                        body.append("&emsp; &emsp; " + message + "<br>");
                    }
                }
                return;

            } else if (status == FlightStatus.WARNING) {
                body.append("&emsp; <a href='http://ngafid.org/protected/flight?flight_id=" + id + "'>flight " + id + "</a> imported with warnings:<br>");
                for (String message : warningMessages) {
                    body.append("&emsp; &emsp; " + message + "<br>");
                }
            } else {
                body.append("&emsp; <a href='http://ngafid.org/protected/flight?flight_id=" + id + "'>flight " + id + "</a> imported OK:<br>");
            }

            for (String message : exceedenceMessages) {
                body.append("&emsp; &emsp; event found: " + message + "<br>");
            }

            for (String message : exceedenceErrorMessages) {
                body.append("&emsp; &emsp; event calculation warning: " + message + "<br>");
            }

            for (String message : proximityMessages) {
                body.append("&emsp; &emsp; proximity event found: " + message + "<br>");
            }

            for (String message : proximityErrorMessages) {
                body.append("&emsp; &emsp; proximity calculation warning: " + message + "<br>");
            }

            for (String message : ttfErrorMessages) {
                body.append("&emsp; &emsp; turn-to-final calculation warning: " + message + "<br>");
            }
        }
    }

    private TreeMap<String, FlightInfo> flightInfoMap = new TreeMap<String, FlightInfo>();
    private String subject;
    private ArrayList<String> recipients;
    private ArrayList<String> bccRecipients;

    int numberEvents = 0;
    int numberEventErrors = 0;
    int numberProximityEvents = 0;
    int numberProximityErrors = 0;
    int numberTTFErrors = 0;

    double importElapsedTime;
    double exceedencesElapsedTime;
    double proximityElapsedTime;
    double proximityAvgTime;
    double proximityAvgTimeMatchTime;
    double proximityAvgLocationMatchTime;

    double ttfElapsedTime;

    private int validFlights;
    private int warningFlights;
    private int errorFlights;

    private boolean importFailed = false;
    private ArrayList<String> importFailedMessages = new ArrayList<String>();

    public UploadProcessedEmail(ArrayList<String> recipients, ArrayList<String> bccRecipients) {
        this.recipients = recipients;
        this.bccRecipients = bccRecipients;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void addImportFailure(String message) {
        importFailed = true;
        importFailedMessages.add(message);
    }

    public void setImportElapsedTime(double importElapsedTime) {
        this.importElapsedTime = importElapsedTime;
    }

    public void setExceedencesElapsedTime(double exceedencesElapsedTime) {
        this.exceedencesElapsedTime = exceedencesElapsedTime;
    }

    public void setProximityElapsedTime(double proximityElapsedTime, double avgTime, double avgTimeMatchTime, double avgLocationMatchTime) {
        this.proximityElapsedTime = proximityElapsedTime;
        this.proximityAvgTime = avgTime;
        this.proximityAvgTimeMatchTime = avgTimeMatchTime;
        this.proximityAvgLocationMatchTime = avgLocationMatchTime;
    }

    public void setTTFElapsedTime(double ttfElapsedTime) {
        this.ttfElapsedTime = ttfElapsedTime;
    }

    public void setValidFlights(int validFlights) {
        this.validFlights = validFlights;
    }

    public void setWarningFlights(int warningFlights) {
        this.warningFlights = warningFlights;
    }

    public void setErrorFlights(int errorFlights) {
        this.errorFlights = errorFlights;
    }

    public void addFlight(String filename, int id, int length) {
        flightInfoMap.put(filename, new FlightInfo(filename, id, length));
    }

    public void flightImportError(String filename, String errorMessage) {
        FlightInfo flightInfo = new FlightInfo(filename);
        flightInfo.setError(errorMessage);

        flightInfoMap.put(filename, flightInfo);
    }

    public void flightImportOK(String filename) {
        flightInfoMap.get(filename).setOK();
    }

    public void flightImportWarning(String filename, String warningMessage) {
        flightInfoMap.get(filename).setWarning(warningMessage);
    }

    public void addExceedence(String filename, String message) {
        flightInfoMap.get(filename).addExceedence(message);
        numberEvents++;
    }

    public void addExceedenceError(String filename, String message) {
        if (flightInfoMap.get(filename) == null) {
            flightInfoMap.put(filename, new FlightInfo(filename));
        }

        flightInfoMap.get(filename).addExceedenceError(message);
        numberEventErrors++;
    }

    public void addProximity(String filename, String message) {
        flightInfoMap.get(filename).addProximity(message);
        numberProximityEvents++;
    }

    public void addProximityError(String filename, String message) {
        flightInfoMap.get(filename).addProximityError(message);
        numberProximityErrors++;
    }

    public void addTTFError(String filename, String message) {
        flightInfoMap.get(filename).addTTFError(message);
        numberTTFErrors++;
    }


    public void sendEmail() {

        StringBuilder body = new StringBuilder();

        body.append("<body><html><br>");
        body.append("importing " + flightInfoMap.size() + " flight files to the database took " + importElapsedTime + "<br>");

        int numberOkFlights = 0;
        int numberWarningFlights = 0;
        int numberErrorFlights = 0;

        for (FlightInfo info : flightInfoMap.values()) {
            if (info.wasOK()) {
                numberOkFlights++;
            } else if (info.wasWarning()) {
                numberWarningFlights++;
            } else {
                numberErrorFlights++;
            }
        }

        body.append("&emsp; " + numberOkFlights + " imported with no issues.<br>");
        body.append("&emsp; " + numberWarningFlights + " imported with warnings.<br>");
        body.append("&emsp; " + numberErrorFlights + " had errors and could not be imported.<br>");
        body.append("<br>");

        int numberImportedFlights = numberOkFlights + numberWarningFlights;
        body.append("calculating flight events for " + numberImportedFlights + " took " + exceedencesElapsedTime + " seconds.<br>");
        body.append("&emsp; " + numberEvents + " events were found.<br>");
        body.append("&emsp; " + numberEventErrors + " events could not be calculated due to data issues.<br>");
        body.append("<br>");

        body.append("calculating proximity events for " + numberImportedFlights + " took " + proximityElapsedTime + " seconds, averaging " + proximityAvgTime + " seconds per flight, time bound matching averaged " + proximityAvgTimeMatchTime + " seconds and location bound matching took " + proximityAvgLocationMatchTime + " seconds on average.<br>");
        body.append("&emsp; " + numberProximityEvents + " proximity events were found.<br>");
        body.append("&emsp; " + numberProximityErrors + " flights could not be processed for proximity due to data issues.<br>");
        body.append("<br>");

        body.append("calculating turn to final information for " + numberImportedFlights + " took " + ttfElapsedTime + " seconds.");
        body.append("&emsp; " + numberTTFErrors + " flights could not be processed for turn-to-final due to data issues.<br>");
        body.append("<br>");

        body.append("flight details:<br>");
        for (FlightInfo info : flightInfoMap.values()) {
            info.getDetails(body);
        }

        body.append("</body></html>");

        SendEmail.sendEmail(recipients, bccRecipients, subject, body.toString(), EmailType.IMPORT_PROCESSED_RECEIPT);
    }
}
