package org.ngafid.flights.dji;

import java.nio.charset.StandardCharsets;

public class DATHeader {

    public enum DroneModel {
        P3I1, P3AP, P3S, I1, P4, MavicPro, P4P, P4A, I2, M200, M600, M100, SPARK, UNKNOWN, MavicAir, S900
    }

    private final DATDJIFile DATDJIFile;
//    private final String firmwareDate;

    public DATHeader(DATDJIFile DATDJIFile) {
        this.DATDJIFile = DATDJIFile;
//        this.firmwareDate = calculateFWDate();
    }

    public DroneModel getDroneModel() {
        DroneModel droneModel;
        DATDJIFile.setClockRate(4500000.0);
        switch (DATDJIFile.memory.get(0)) {
            case 5:

                droneModel = DroneModel.M100;
                DATDJIFile.setClockRate(85000000.0);
                break;
            case 6:
                droneModel = DroneModel.P3I1;
                DATDJIFile.setClockRate(600.0);
                break;
            case 11:
                droneModel = DroneModel.P4;
                break;
            case 14:
                droneModel = DroneModel.M600;
                break;
            case 23:
                droneModel = DroneModel.M600;
                break;
            case 16:
                droneModel = DroneModel.MavicPro;
                break;
            case 17:
                droneModel = DroneModel.I2;
                break;
            case 18:
                droneModel = DroneModel.P4P;
                break;
            case 20:
                droneModel = DroneModel.S900;
                break;
            case 21:
                droneModel = DroneModel.SPARK;
                break;
            case 24:
                droneModel = DroneModel.MavicAir;
                break;
            case 25:
                droneModel = DroneModel.M200;
                break;
            case 27:
                droneModel = DroneModel.P4A;
                break;
            default:
                droneModel = DroneModel.UNKNOWN;
                break;

        }

        return droneModel;
    }


    private String calculateFWDate() {
        byte[] fwDateByteArr = new byte[13];
        int j = 0;
        for (int i = 21; i < 33; i++) {
            fwDateByteArr[j++] = DATDJIFile.memory.get(i);
        }

        return new String(fwDateByteArr, StandardCharsets.UTF_8);
    }

    public DATDJIFile getDatFile() {
        return DATDJIFile;
    }

//    public String getFirmwareDate() {
//        return firmwareDate;
//    }

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

            case P3S:
                return "P3Standard";

            case P4:
                return "P4";

            case P4P:
                return "P4Pro";

            case P4A:
                return "P4Adv";

            default:
                return "Unknown";

        }
    }
}
