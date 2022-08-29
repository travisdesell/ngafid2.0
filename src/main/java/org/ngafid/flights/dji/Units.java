package org.ngafid.flights.dji;

public class Units {
    // TODO: See if all of these are necessary

    public static final Units metersPerSec = new Units("meters/Sec");

    public static final Units metersPerSec2 = new Units("meters/Sec2");

    public static final Units degreesC = new Units("degrees C");

    public static final Units noUnits = new Units("");

    public static final Units meters = new Units("meters");

    public static final Units msec = new Units("milliSeconds");

    public static final Units gpsHealth = new Units("0,5");

    public static final Units rpm = new Units("rpm");

    public static final Units volts = new Units("volts");

    public static final Units percentage = new Units("%");

    public static final Units degrees = new Units("degrees");

    public static final Units gpsCoord = new Units("degrees");

    public static final Units degrees180 = new Units("degrees [-180,180]");

    public static final Units degrees360 = new Units("degrees [0,360]");

    public static final Units amps = new Units("Amperes");

    public static final Units watts = new Units("Watts");

    public static final Units wattsSecs = new Units("WattSecs");

    public static final Units wattsSecsPerDist = new Units("WattSecs/Dist");

    public static final Units wattsPerVel = new Units("Watts/Vel");

    public static final Units degreesPerSec = new Units("degrees/Sec");

    public static final Units aTesla = new Units("aTesla");

    public static final Units G = new Units("G");

    public static final Units controlStick = new Units("-10000,+10000");

    public static final Units seconds = new Units("Seconds");

    public static final Units mAh = new Units("milliAmpHours");

    String name = "";

    private Units(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

}
