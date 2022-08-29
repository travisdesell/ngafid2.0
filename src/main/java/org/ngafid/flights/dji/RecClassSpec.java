package org.ngafid.flights.dji;

public class RecClassSpec extends RecSpec {

    Class<Record> recClass = null;

    int[] lengths = null;

    @SuppressWarnings("unchecked")
    public RecClassSpec(Class<?> recClass, int id, int length) {
        super(id, length);
        this.recClass = (Class<Record>) recClass;
        if (length == -1) {
            setRecType(RecType.STRING);
        }
    }

    @SuppressWarnings("unchecked")
    public RecClassSpec(Class<?> recClass, int id, int... lengths) {
        super(id, -1);
        this.recClass = (Class<Record>) recClass;
        this.lengths = new int[lengths.length];
        System.arraycopy(lengths, 0, this.lengths, 0, lengths.length);
    }

    public boolean lengthOK(int l) {
        if (getRecType() == RecType.STRING) {
            return true;
        }
        if (l == getLength()) {
            return true;
        }
        if (lengths != null) {
            for (int length : lengths) {
                if (l == length) {
                    return true;
                }
            }
        }
        return false;
    }

    public Class<Record> getRecClass() {
        return recClass;
    }

    public String toString() {
        return recClass.getName();
    }

}
