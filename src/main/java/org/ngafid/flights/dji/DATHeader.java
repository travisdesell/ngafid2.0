package org.ngafid.flights.dji;

import java.io.UnsupportedEncodingException;

public class DATHeader {

    public enum DroneModel {
        P3I1, P3AP, P3S, I1, P4, MavicPro, P4P, P4A, I2, M200, M600, M100, SPARK, UNKNOWN, MavicAir, S900
    }

    private final DatFile datFile;

    public DATHeader(DatFile datFile) {
        this.datFile = datFile;
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

    public static String toString(DroneModel droneModel) {
        switch (droneModel) {
            case I1:
                return "Inspire1";
            case I2:
                return "Inspire2";
            case M100:
                return "Matrice100";
            case M200:
                return "Matrice200";
            case M600:
                return "Matrice600";
            case MavicPro:
                return "MavicPro";
            case MavicAir:
                return "MavicAir";
            case SPARK:
                return "Spark";
            case S900:
                return "S900";
            case P3AP:
                return "P3Adv/Pro";
            case P3I1:
                break;
            case P3S:
                return "P3Standard";
            case P4:
                return "P4";
            case P4P:
                return "P4Pro";
            case P4A:
                return "P4Adv";
            case UNKNOWN:
                break;
            default:
                break;
        }
        return "Unknown";
    }

    public String getFWDate() {
        byte x[] = new byte[13];
        int j = 0;
        for (int i = 21; i < 33; i++) {
            x[j++] = datFile.memory.get(i);
        }
        String retv;
        try {
            retv = new String(x, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            retv = "";
        }
        return retv;
    }

}
