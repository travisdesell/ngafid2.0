package org.ngafid.flights.dji;

import java.util.Vector;

public class RecordDef extends DATRecord {

    Vector<Field> fields = new Vector<>();

    public void addField(Field field) {
        setLength(getLength() + field.getSize());
        fields.add(field);
    }

    public RecordDef(String name, int id, RecSpec.RecType recType) {
        super(name, id, recType);
    }

    public String getNameWithId() {
        return getName() + "_" + getId();
    }

    public String getNameWithLengthAndId() {
        return getName() + "_" + getLength() + "_" + getId();
    }

    public int getNumFields() {
        return fields.size();
    }

    public Vector<Field> getFields() {
        return fields;
    }

    public String toString() {
        String retv = "";
        retv = "RecordDef " + getName() + " " + getId();
        //        for (int i = 0; i < fields.size(); i++) {
        //            retv += fields.get(i) + "\n";
        //        }
        return retv;
    }

    public String getClassDescription() {
        return "RecordDef " + getNameWithId() + " /" + getLength();
    }

    private boolean valid = false;

    private DATSignal IntSignal = null;

    private DATSignal FloatSignal = null;

    private DATSignal DoubleSignal = null;

    private Number[] values = null;

    public void init(DATConvert convertDatV3) {
        this.convertDat = convertDatV3;
        datFile = this.convertDat.getDatFile();
        this.csvWriter = convertDat.csvWriter;
        IntSignal = DATSignal.SeriesInt(getName(), "", null, Units.noUnits);
        FloatSignal = DATSignal.SeriesFloat(getName(), "", null, Units.noUnits);
        DoubleSignal = DATSignal.SeriesDouble(getName(), "", null, Units.noUnits);
        values = new Number[getNumFields()];
        valid = false;
    }

    @Override
    public void process(Payload payload) {
        super.process(payload);
        try {
            valid = true;
            int offset = 0;
            for (int fieldNum = 0; fieldNum < fields.size(); fieldNum++) {
                Field field = fields.get(fieldNum);
                switch (field.getType()) {
                    case duble:
                        values[fieldNum] = payload.getDouble(offset);
                        break;
                    case expr:
                        break;
                    case fp32:
                        values[fieldNum] = payload.getFloat(offset);
                        break;
                    case int16_t:
                        values[fieldNum] = payload.getShort(offset);
                        break;
                    case int32_t:
                        values[fieldNum] = payload.getInt(offset);
                        break;
                    case int8_t:
                        values[fieldNum] = payload.getByte(offset);
                        break;
                    case uint16_t:
                        values[fieldNum] = payload.getUnsignedShort(offset);
                        break;
                    case uint32_t:
                        values[fieldNum] = payload.getUnsignedInt(offset);
                        break;
                    case uint8_t:
                        values[fieldNum] = payload.getUnsignedByte(offset);
                        break;
                    default:
                        throw new RuntimeException("process(Payload payload) ");

                }
                offset += field.getSize();
            }
        } catch (Exception e) {
            RecordException(e);
            //DatConLog.Exception(e);
        }
    }

    @Override
    public void printCols(DATConvert.lineType lineT) {
        try {
            for (int fieldNum = 0; fieldNum < fields.size(); fieldNum++) {
                Field field = fields.get(fieldNum);
                switch (field.getType()) {
                    case duble:
                        printCSVValue(values[fieldNum], DoubleSignal, field.getName(), lineT, valid);
                        break;
                    case expr:
                        break;
                    case fp32:
                        printCSVValue(values[fieldNum], FloatSignal, field.getName(), lineT, valid);
                        break;
                    case int16_t:
                    case uint8_t:
                    case uint32_t:
                    case uint16_t:
                    case int8_t:
                    case int32_t:
                        printCSVValue(values[fieldNum], IntSignal, field.getName(), lineT, valid);
                        break;
                    default:
                        throw new RuntimeException("printCols(lineType lineT) ");

                }
            }
        } catch (Exception e) {
//            DatConLog.Exception(e);
        }
    }

}
