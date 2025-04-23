package org.ngafid.core.flights;

import org.ngafid.core.util.NormalizedColumn;

import java.sql.Connection;
import java.sql.SQLException;

public final class TypeName extends NormalizedColumn<TypeName> {
    public TypeName(String name) {
        super(name);
    }

    public TypeName(int id) {
        super(id);
    }

    public TypeName(int id, String name) {
        super(id, name);
    }

    public TypeName(Connection connection, int id) throws SQLException {
        super(connection, id);
    }

    public TypeName(Connection connection, String name) throws SQLException {
        super(connection, name);
    }

    @Override
    protected String getTableName() {
        return "data_type_names";
    }

}
