package org.ngafid.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class H2Database {

    private static HikariDataSource CONNECTION_POOL = null;

    private static final Logger LOG = Logger.getLogger(H2Database.class.getName());

    static {
        createConnectionPool();
        try {
            populateDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return CONNECTION_POOL.getConnection();
    }

    private static void createConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER,VALUE,YEAR,MONTH;DATABASE_TO_UPPER=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setPoolName("H2Pool");

        CONNECTION_POOL = new HikariDataSource(config);
    }

    private static void populateDatabase() throws SQLException {
        try (Connection connection = CONNECTION_POOL.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase("ngafid-db/src/test-changelog-root.xml",
                    new DirectoryResourceAccessor(Path.of("../")), database);

            liquibase.update(new Contexts());
        } catch (LiquibaseException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
