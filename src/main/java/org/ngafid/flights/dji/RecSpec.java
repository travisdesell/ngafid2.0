package org.ngafid.flights.dji;

public class RecSpec {

    int id = 0;

    public void setId(int iD) {
        id = iD;
    }

    public int getId() {
        return id;
    }

    public boolean isId(int id) {
        return (this.id == id);
    }

    private int length = 0;

    protected void setLength(int l) {
        length = l;
    }

    public int getLength() {
        return length;
    }

    private String name = "N/A";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum RecType {
        BINARY, STRING
    }

    protected RecSpec.RecType recType = RecSpec.RecType.BINARY;

    public RecSpec(int t, int length) {
        id = t;
        this.length = length;
    }

    public RecSpec.RecType getRecType() {
        return recType;
    }

    protected void setRecType(RecType recType) {
        this.recType = recType;
    }

    public RecSpec() {}

    public RecSpec(String name, int id, RecType recType) {
        this.name = name;
        this.id = id;
        this.recType = recType;
    }

    @Override
    public boolean equals(Object z) {
        if (!(z instanceof RecSpec))
            return false;
        return (((RecSpec) z).id == id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    public String toString() {
        return id + "/" + length;
    }

    public String getDescription() {
        return "LO " + ((0XFF) & id) + " ID " + id + " length " + length;
    }

}
