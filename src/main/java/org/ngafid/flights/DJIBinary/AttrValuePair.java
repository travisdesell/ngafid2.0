package org.ngafid.flights.DJIBinary;

import java.io.UnsupportedEncodingException;

public class AttrValuePair {
    private String attr;

    private String value;

    public AttrValuePair(String attr, String value) {
        this.attr = attr;
        StringBuilder cleanValue = new StringBuilder();
        try {
            byte dirtyBytes[] = value.getBytes("UTF-8");
            for (int i = 0; i < dirtyBytes.length; i++) {
                byte b = dirtyBytes[i];
                if (0x20 <= b && b <= 0x7E && b != 0x2C) {
                    cleanValue.append(new String(new byte[]{b}));
                }
            }
        } catch (UnsupportedEncodingException e) {
            // just ignore any errors
        }
        this.value = cleanValue.toString();
    }

    public String getAttr() {
        return attr;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
