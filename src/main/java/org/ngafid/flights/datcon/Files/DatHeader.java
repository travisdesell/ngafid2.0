package org.ngafid.flights.datcon.Files;

import org.ngafid.flights.datcon.Files.DatFile;

import java.io.UnsupportedEncodingException;

public class DatHeader {

    public enum AcType {
        P3I1, P3AP, P3S, I1, P4, MavicPro, P4P, P4A, I2, M200, M600, M100, SPARK, UNKNOWN, MavicAir, S900
    }

    private DatFile datFile;

    public DatHeader(DatFile datFile) {
        this.datFile = datFile;
    }

    public AcType getAcType() {
        AcType acType;
        datFile.setClockRate(4500000.0);
        switch (datFile.memory.get(0)) {
        case 05: {
            acType = AcType.M100;
            datFile.setClockRate(85000000.0);
            break;
        }
        case 06: {
            acType = AcType.P3I1;
            datFile.setClockRate(600.0);
            break;
        }
        case 11: {
            acType = AcType.P4;
            break;
        }
        case 14: {
            acType = AcType.M600;
            break;
        }
        case 16: {
            acType = AcType.MavicPro;
            break;
        }
        case 17: {
            acType = AcType.I2;
            break;
        }
        case 18: {
            acType = AcType.P4P;
            break;
        }
        case 20: {
            acType = AcType.S900;
            break;
        }
        case 21: {
            acType = AcType.SPARK;
            break;
        }
        case 23: { // M600 Pro
            acType = AcType.M600;
            break;
        }
        case 24: {
            acType = AcType.MavicAir;
            break;
        }
        case 25: {
            acType = AcType.M200;
            break;
        }
        case 27: {
            acType = AcType.P4A;
            break;
        }
        default: {
            acType = AcType.UNKNOWN;
            break;
        }
        }
        return acType;
    }

    public static String toString(AcType acType) {
        switch (acType) {
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
