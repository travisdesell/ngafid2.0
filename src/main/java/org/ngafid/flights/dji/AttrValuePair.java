package org.ngafid.flights.dji;

import java.nio.charset.StandardCharsets;

public class AttrValuePair {
    private final String attr;

    private String value;

    public AttrValuePair(String attr, String value) {
        this.attr = attr;
        StringBuilder cleanValue = new StringBuilder();
        byte[] dirtyBytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : dirtyBytes) {
            if (0x20 <= b && b <= 0x7E && b != 0x2C) {
                cleanValue.append(new String(new byte[]{b}));
            }
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
