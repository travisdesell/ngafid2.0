package org.ngafid.core.flights;

import java.sql.Connection;
import java.sql.SQLException;
import org.ngafid.core.util.NormalizedColumn;

public final class TypeName extends NormalizedColumn<TypeName> {
    /**
     * Constructs a data-type name from its name, resolving the id from the in-memory cache only (no database lookup).
     *
     * @param name the data-type name
     */
    public TypeName(String name) {
        super(name);
    }

    /**
     * Constructs a data-type name from its id, resolving the name from the in-memory cache only (no database lookup).
     *
     * @param id the data-type name id
     */
    public TypeName(int id) {
        super(id);
    }

    /**
     * Constructs a data-type name from an already-known id and name, without consulting the cache or database.
     *
     * @param id the data-type name id
     * @param name the data-type name
     */
    public TypeName(int id, String name) {
        super(id, name);
    }

    /**
     * Constructs a data-type name from its id, resolving the name from the database on a cache miss and caching it.
     *
     * @param connection the database connection used to resolve the name
     * @param id the data-type name id
     * @throws SQLException if the lookup fails
     */
    public TypeName(Connection connection, int id) throws SQLException {
        super(connection, id);
    }

    /**
     * Constructs a data-type name from its name, resolving the id from the database on a cache miss (inserting the name
     * if it does not yet exist) and caching the mapping.
     *
     * @param connection the database connection used to resolve or insert the name
     * @param name the data-type name
     * @throws SQLException if the lookup or insertion fails
     */
    public TypeName(Connection connection, String name) throws SQLException {
        super(connection, name);
    }

    @Override
    protected String getTableName() {
        return "data_type_names";
    }
}
