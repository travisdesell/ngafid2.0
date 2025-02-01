package org.ngafid.flights;

import org.ngafid.common.NormalizedColumn;

import java.sql.Connection;
import java.sql.SQLException;

public final class TypeName extends NormalizedColumn<TypeName> {
    private static Class<TypeName> clazz = TypeName.class;

    public TypeName(String name) {
        super(clazz, name);
    }

    public TypeName(int id) {
        super(clazz, id);
    }

    public TypeName(int id, String name) {
        super(id, name);
    }

    public TypeName(Connection connection, int id) throws SQLException {
        super(clazz, connection, id);
    }

    public TypeName(Connection connection, String name) throws SQLException {
        super(clazz, connection, name);
    }

    @Override
    protected String getTableName() {
        return "data_type_names";
    }

}
