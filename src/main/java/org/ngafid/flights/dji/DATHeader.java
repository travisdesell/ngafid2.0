package org.ngafid.flights.dji;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class DATHeader {

    public enum DroneModel {
        P3I1, P3AP, P3S, I1, P4, MavicPro, P4P, P4A, I2, M200, M600, M100, SPARK, UNKNOWN, MavicAir, S900
    }

    private final DatFile datFile;
    private final String firmwareDate;

    public DATHeader(DatFile datFile) {
        this.datFile = datFile;
        this.firmwareDate = calculateFWDate();
    }

    public DroneModel getDroneModel() {
        DroneModel droneModel;
        datFile.setClockRate(4500000.0);
        switch (datFile.memory.get(0)) {
            case 5 -> {
                droneModel = DroneModel.M100;
                datFile.setClockRate(85000000.0);
            }
            case 6 -> {
                droneModel = DroneModel.P3I1;
                datFile.setClockRate(600.0);
            }
            case 11 -> droneModel = DroneModel.P4;
            case 14, 23 -> droneModel = DroneModel.M600;
            case 16 -> droneModel = DroneModel.MavicPro;
            case 17 -> droneModel = DroneModel.I2;
            case 18 -> droneModel = DroneModel.P4P;
            case 20 -> droneModel = DroneModel.S900;
            case 21 -> droneModel = DroneModel.SPARK;
            case 24 -> droneModel = DroneModel.MavicAir;
            case 25 -> droneModel = DroneModel.M200;
            case 27 -> droneModel = DroneModel.P4A;
            default -> droneModel = DroneModel.UNKNOWN;

        }

        return droneModel;
    }


    private String calculateFWDate() {
        byte[] fwDateByteArr = new byte[13];
        int j = 0;
        for (int i = 21; i < 33; i++) {
            fwDateByteArr[j++] = datFile.memory.get(i);
        }

        return new String(fwDateByteArr, StandardCharsets.UTF_8);
    }

    public DatFile getDatFile() {
        return datFile;
    }

    public String getFirmwareDate() {
        return firmwareDate;
    }

    public static String toString(DroneModel droneModel) {
        String str;

        switch (droneModel) {
            case I1 -> str = "Inspire1";
            case I2 -> str = "Inspire2";
            case M100 -> str = "Matrice100";
            case M200 -> str = "Matrice200";
            case M600 -> str = "Matrice600";
            case MavicPro -> str = "MavicPro";
            case MavicAir -> str = "MavicAir";
            case SPARK -> str = "Spark";
            case S900 -> str = "S900";
            case P3AP -> str = "P3Adv/Pro";
            case P3S -> str = "P3Standard";
            case P4 -> str = "P4";
            case P4P -> str = "P4Pro";
            case P4A -> str = "P4Adv";
            default -> str = "Unknown";
        }

        return str;
    }
}
