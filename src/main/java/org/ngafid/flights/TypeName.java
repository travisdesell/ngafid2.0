package org.ngafid.flights;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.common.NormalizedColumn;

public class TypeName extends NormalizedColumn<TypeName> {

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
