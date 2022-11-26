package org.ngafid.flights.datcon.DatConRecs.String;

import DatConRecs.String.RecDefs_65533;
import DatConRecs.String.RecFlyLog_32768;
import DatConRecs.String.Sys_cfg_65535;
import Files.RecClassSpec;

import java.util.Vector;

public class Dictionary {

    public static Vector<RecClassSpec> entries = new Vector<RecClassSpec>();
    static {

        entries.add(new RecClassSpec(Sys_cfg_65535.class, 65535, -1));
        entries.add(new RecClassSpec(RecDefs_65533.class, 65533, -1));
        entries.add(new RecClassSpec(RecFlyLog_32768.class, 32768, -1));
    }

}
