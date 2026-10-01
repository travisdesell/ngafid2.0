package org.ngafid.core.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngafid.core.util.NormalizedColumn;
import org.ngafid.core.util.filters.Pair;

public final class Airframes {
    private Airframes() {}

    private static final Logger LOG = Logger.getLogger(Airframes.class.getName());

    /*
     * if (airframeName.equals("Cessna 172R") || airframeName.equals("Cessna 172S")
     * || airframeName.equals("Cessna 172T") || airframeName.equals("Cessna 182T")
     * || airframeName.equals("Cessna T182T") ||
     * airframeName.equals("Cessna Model 525") || airframeName.equals("Cirrus SR20")
     * || airframeName.equals("Cirrus SR22") || airframeName.equals("Diamond DA40")
     * || airframeName.equals("Diamond DA 40 F") ||
     * airframeName.equals("Diamond DA40NG") ||
     * airframeName.equals("Diamond DA42NG") || airframeName.equals("PA-28-181") ||
     * airframeName.equals("PA-44-180") ||
     * airframeName.equals("Piper PA-46-500TP Meridian") ||
     * airframeName.contains("Garmin") || airframeName.equals("Quest Kodiak 100") ||
     * airframeName.equals("Cessna 400") ||
     * airframeName.equals("Beechcraft A36/G36") ||
     * airframeName.equals("Beechcraft G58") ||
     * airframeName.equals("Beechcraft C90A King Air") ||
     * airframeName.equals("Cessna T206H")) {
     */

    /**
     * {@link Airframes} names
     * <p>
     * TODO: In the future, we may want to consider using Set<String> reather than
     * hardcoded strings. This would make our code more robust to varying airframe
     * names
     **/
    public static final String AIRFRAME_SCAN_EAGLE = "ScanEagle";

    public static final String AIRFRAME_DJI = "DJI";

    public static final String AIRFRAME_CESSNA_172S = "Cessna 172S";
    public static final String AIRFRAME_CESSNA_172R = "Cessna 172R";
    public static final String AIRFRAME_CESSNA_172T = "Cessna 172T";
    public static final String AIRFRAME_CESSNA_400 = "Cessna 400";
    public static final String AIRFRAME_CESSNA_525 = "Cessna 525";
    public static final String AIRFRAME_CESSNA_MODEL_525 = "Cessna Model 525";
    public static final String AIRFRAME_CESSNA_T182T = "Cessna T182T";
    public static final String AIRFRAME_CESSNA_182T = "Cessna 182T";

    public static final String AIRFRAME_PA_28_181 = "PA-28-181";
    public static final String AIRFRAME_PA_44_180 = "PA-44-180";
    public static final String AIRFRAME_PIPER_PA_46_500TP_MERIDIAN = "Piper PA-46-500TP Meridian";

    public static final String AIRFRAME_CIRRUS_SR20 = "Cirrus SR20";
    public static final String AIRFRAME_CIRRUS_SR22 = "Cirrus SR22";

    public static final String AIRFRAME_BEECHCRAFT_A36_G36 = "Beechcraft A36/G36";
    public static final String AIRFRAME_BEECHCRAFT_G58 = "Beechcraft G58";

    public static final String AIRFRAME_DIAMOND_DA_40 = "Diamond DA 40";
    public static final String AIRFRAME_DIAMOND_DA40 = "Diamond DA40";
    public static final String AIRFRAME_DIAMOND_DA40NG = "Diamond DA40NG";
    public static final String AIRFRAME_DIAMOND_DA42NG = "Diamond DA42NG";
    public static final String AIRFRAME_DIAMOND_DA_40_F = "Diamond DA 40 F";

    public static final String AIRFRAME_QUEST_KODIAK_100 = "Quest Kodiak 100";

    private static HashMap<String, Integer> nameIdMap = new HashMap<>();
    private static HashMap<Integer, String> airframeNameMap = new HashMap<>();
    private static HashMap<String, Integer> typeIdMap = new HashMap<>();
    private static HashMap<Integer, String> airframeTypeMap = new HashMap<>();

    private static HashSet<String> fleetAirframes = new HashSet<>();

    public static final Set<String> FIXED_WING_AIRFRAMES = Collections.unmodifiableSet(Set.<String>of(
            AIRFRAME_CESSNA_172R,
            AIRFRAME_CESSNA_172S,
            AIRFRAME_CESSNA_172T,
            AIRFRAME_CESSNA_182T,
            AIRFRAME_CESSNA_T182T,
            AIRFRAME_CESSNA_MODEL_525,
            AIRFRAME_CIRRUS_SR20,
            AIRFRAME_CIRRUS_SR22,
            AIRFRAME_DIAMOND_DA40,
            AIRFRAME_DIAMOND_DA_40_F,
            AIRFRAME_DIAMOND_DA40NG,
            AIRFRAME_DIAMOND_DA42NG,
            AIRFRAME_PA_28_181,
            AIRFRAME_PA_44_180,
            AIRFRAME_PIPER_PA_46_500TP_MERIDIAN,
            AIRFRAME_QUEST_KODIAK_100,
            AIRFRAME_CESSNA_400,
            AIRFRAME_BEECHCRAFT_A36_G36,
            AIRFRAME_BEECHCRAFT_G58));

    // CHECKSTYLE:OFF
    public static final Set<String> ROTORCRAFT = Set.of("R44", "Robinson R44");

    // CHECKSTYLE:ON

    /** Canonical rotorcraft codes in {@code tail_airframe_registry} / {@code airframes}. */
    public static final Set<String> ROTORCRAFT_AIRFRAME_CODES =
            Set.of("407", "AS350", "AW109", "AW119", "AW139", "BK117", "EC130", "EC135", "MH60", "MH65", "R44");

    /**
     * Garmin {@code #airframe_info} {@code airframe_name} values mapped to {@link #ROTORCRAFT_AIRFRAME_CODES}.
     * Keys are lower-case for case-insensitive lookup.
     */
    public static final Map<String, String> GARMIN_ROTORCRAFT_ALIASES = Map.ofEntries(
            Map.entry("bell 407", "407"),
            Map.entry("robinson r44", "R44"),
            Map.entry("robinson r44 cadet", "R44"),
            Map.entry("robinson r44 raven i", "R44"),
            Map.entry("airbus as350", "AS350"),
            Map.entry("eurocopter as350", "AS350"),
            Map.entry("as350 b3e", "AS350"),
            Map.entry("as350 b3", "AS350"),
            Map.entry("airbus ec130", "EC130"),
            Map.entry("eurocopter ec130", "EC130"),
            Map.entry("ec130 t2", "EC130"),
            Map.entry("airbus ec135", "EC135"),
            Map.entry("eurocopter ec135", "EC135"),
            Map.entry("agusta aw109", "AW109"),
            Map.entry("aw109 power", "AW109"),
            Map.entry("aw119", "AW119"),
            Map.entry("aw-119", "AW119"),
            Map.entry("agusta aw-119", "AW119"),
            Map.entry("aw119kx", "AW119"),
            Map.entry("aw-109", "AW109"),
            Map.entry("aw-139", "AW139"),
            Map.entry("aw139", "AW139"),
            Map.entry("bk117", "BK117"),
            Map.entry("bk 117", "BK117"),
            Map.entry("mh-60", "MH60"),
            Map.entry("mh60", "MH60"),
            Map.entry("sikorsky mh-60", "MH60"),
            Map.entry("mh-65", "MH65"),
            Map.entry("mh65", "MH65"),
            Map.entry("eurocopter mh-65", "MH65"));

    /**
     * Maps a Garmin recorder {@code airframe_name} to a rotorcraft airframe code, or empty when unknown.
     *
     * @param recorderName the Garmin recorder {@code airframe_name} to resolve
     * @return the matching rotorcraft airframe code, or {@link Optional#empty()} if none matches
     */
    public static Optional<String> resolveGarminRotorcraftAirframeCode(String recorderName) {
        if (recorderName == null || recorderName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = recorderName.trim();
        String key = trimmed.toLowerCase(Locale.ROOT);
        String alias = GARMIN_ROTORCRAFT_ALIASES.get(key);
        if (alias == null) {
            alias = GARMIN_ROTORCRAFT_ALIASES.get(key.replace("-", ""));
        }
        if (alias != null) {
            return Optional.of(alias);
        }
        String codeKey = trimmed.replace("-", "");
        if (ROTORCRAFT_AIRFRAME_CODES.contains(codeKey)) {
            return Optional.of(codeKey);
        }
        if (ROTORCRAFT_AIRFRAME_CODES.contains(trimmed)) {
            return Optional.of(trimmed);
        }
        return Optional.empty();
    }

    public record AliasKey(String name, int fleetId) {}

    /**
     * Creates an {@link AliasKey} that applies to all fleets (fleet id -1), used for airframe-name aliases that are
     * not fleet-specific.
     *
     * @param name the airframe name to alias
     * @return an all-fleets alias key for the name
     */
    public static AliasKey defaultAlias(String name) {
        return new AliasKey(name, -1);
    }

    // CHECKSTYLE:OFF
    public static Map<AliasKey, String> AIRFRAME_ALIASES = Map.ofEntries(
            Map.entry(defaultAlias("Unknown Aircraft"), ""),
            // Map.entry(defaultAlias("Garmin Flight Display"), ""),    [EX]
            Map.entry(defaultAlias("Diamond DA 40"), "Diamond DA40"),
            Map.entry(new AliasKey("Garmin Flight Display", 1), "R44"),
            Map.entry(new AliasKey("Robinson R44 Raven I", 1), "R44"),
            Map.entry(defaultAlias("Robinson R44"), "R44"),
            Map.entry(defaultAlias("Robinson R44 Cadet"), "R44"),
            Map.entry(defaultAlias("Cirrus SR22 (3600 GW)"), "Cirrus SR22"));

    // CHECKSTYLE:ON
    public static class Type extends NormalizedColumn<Type> {
        @Override
        protected String getTableName() {
            return "airframe_types";
        }

        /**
         * Creates an unresolved airframe type from its name (no database id assigned yet).
         *
         * @param name the airframe-type name
         */
        public Type(String name) {
            super(name);
        }

        /**
         * Resolves an airframe type from its name, looking up (or assigning) its database id.
         *
         * @param connection the database connection
         * @param name the airframe-type name
         * @throws SQLException if the lookup or insert fails
         */
        public Type(Connection connection, String name) throws SQLException {
            super(connection, name);
        }

        /**
         * Resolves an airframe type from its database id, looking up the corresponding name.
         *
         * @param connection the database connection
         * @param id the airframe-type id
         * @throws SQLException if the lookup fails
         */
        public Type(Connection connection, int id) throws SQLException {
            super(connection, id);
        }
    }

    public static class Airframe {
        private static final ConcurrentHashMap<String, Pair<Type, Integer>> NAME_TO_TYPE_AND_ID =
                new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<Integer, Pair<Type, String>> ID_TO_TYPE_AND_NAME =
                new ConcurrentHashMap<>();

        private int id;
        private String name;
        private Type type;

        /**
         * Resolves an airframe by its name, looking up its id and type from the database.
         *
         * @param connection the database connection
         * @param airframeName the airframe name to resolve
         * @return the resolved airframe
         * @throws SQLException if the lookup fails
         */
        public static Airframe getAirframeByName(Connection connection, String airframeName) throws SQLException {
            return new Airframe(connection, airframeName, null);
        }

        /**
         * Creates an in-memory airframe (id -1, no database lookup) from a name and type.
         *
         * @param name the airframe name
         * @param type the airframe type
         */
        public Airframe(String name, Type type) {
            this.id = -1;
            this.name = name;
            this.type = type;
        }

        /**
         * Resolves an airframe from its name, filling in its id and type from the database (or a process-wide cache
         * on repeat lookups of the same name).
         *
         * @param connection the database connection
         * @param name the airframe name
         * @param type the airframe type, or null to resolve it from the database
         * @throws SQLException if the lookup fails
         */
        public Airframe(Connection connection, String name, Type type) throws SQLException {
            this.name = name;
            this.id = -1;
            this.type = type;

            if (!NAME_TO_TYPE_AND_ID.containsKey(name)) {
                getIdAndType(connection);
                NAME_TO_TYPE_AND_ID.put(name, new Pair<>(this.type, this.id));
            } else {
                Pair<Type, Integer> typeAndId = NAME_TO_TYPE_AND_ID.get(name);
                this.type = typeAndId.first();
                this.id = typeAndId.second();
            }
        }

        /**
         * Resolves an airframe from its database id, filling in its name and type from the database (or a
         * process-wide cache on repeat lookups of the same id).
         *
         * @param connection the database connection
         * @param id the airframe id
         * @throws SQLException if the lookup fails
         */
        public Airframe(Connection connection, int id) throws SQLException {
            this.id = id;
            this.name = null;
            this.type = null;

            if (!ID_TO_TYPE_AND_NAME.containsKey(id)) {
                getNameAndType(connection);
                ID_TO_TYPE_AND_NAME.put(id, new Pair<>(type, name));
            } else {
                Pair<Type, String> typeAndName = ID_TO_TYPE_AND_NAME.get(id);
                this.type = typeAndName.first();
                this.name = typeAndName.second();
            }
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public int getId() {
            return id;
        }

        private void getIdAndType(Connection connection) throws SQLException {
            try (PreparedStatement query =
                    connection.prepareStatement("SELECT id, type_id FROM airframes WHERE airframe = ?")) {
                query.setString(1, name);

                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getInt("id");
                        this.type = new Type(connection, rs.getInt("type_id"));
                    } else {
                        if (type != null) generateNewId(connection);
                        else throw new SQLException("Airframe not found");
                    }
                }
            }
        }

        void generateNewId(Connection connection) throws SQLException {
            try (PreparedStatement query =
                    connection.prepareStatement("INSERT IGNORE INTO airframes (airframe, type_id) VALUES (?, ?)")) {
                this.type = new Type(connection, this.type.getName());
                query.setString(1, name);
                query.setInt(2, type.getId());

                int rs = query.executeUpdate();
                getIdAndType(connection);
            }
        }

        private void getNameAndType(Connection connection) throws SQLException {
            try (PreparedStatement query =
                    connection.prepareStatement("SELECT airframe, type_id FROM airframes WHERE id = ?")) {
                query.setInt(1, id);

                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        this.name = rs.getString("airframe");
                        this.type = new Type(connection, rs.getInt("type_id"));
                    } else {
                        throw new SQLException("Unrecognized Airframe id: " + id);
                    }
                }
            }
        }
    }

    /**
     * Associates an airframe with a fleet by inserting a {@code fleet_airframes} row, unless that association has
     * already been recorded (tracked by an in-memory cache to avoid redundant inserts).
     *
     * @param connection the database connection
     * @param airframeId the airframe id
     * @param fleetId the fleet id to associate the airframe with
     * @throws SQLException if the insert fails
     */
    public static void setAirframeFleet(Connection connection, int airframeId, int fleetId) throws SQLException {
        String key = airframeId + "-" + fleetId;

        // this was already inserted to the database
        if (fleetAirframes.contains(key)) return;
        else {

            String queryString = "REPLACE INTO fleet_airframes (fleet_id, airframe_id) VALUES (?, ?)";

            try (PreparedStatement query = connection.prepareStatement(queryString)) {
                query.setInt(1, fleetId);
                query.setInt(2, airframeId);

                // LOG.info(query.toString());
                query.executeUpdate();

                fleetAirframes.add(key);
            }
        }
    }

    /**
     * Returns the names of the airframes associated with a fleet (joined through {@code fleet_airframes}), ordered by
     * name.
     *
     * @param connection the database connection
     * @param fleetId the fleet whose airframes to list
     * @return the fleet's airframe names
     * @throws SQLException if the query fails
     */
    public static ArrayList<String> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> airframes = new ArrayList<>();

        String queryString = "SELECT airframe FROM airframes INNER JOIN fleet_airframes ON "
                + "airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);

            try (ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    // airframe existed in the database, return the id
                    String airframe = resultSet.getString(1);
                    airframes.add(airframe);
                }
            }

            return airframes;
        }
    }

    public record AirframeNameID(String name, int id) {
        /*...*/
    }

    public static final int FLEET_ID_ALL = -1;

    /**
     * Returns every airframe (across all fleets) as name/id pairs.
     *
     * @param connection the database connection
     * @return the airframe name/id pairs for all fleets
     * @throws SQLException if the query fails
     */
    public static AirframeNameID[] getAllWithIds(Connection connection) throws SQLException {
        return getAllWithIds(connection, FLEET_ID_ALL);
    }

    /**
     * Returns the airframes as name/id pairs, scoped to a fleet (or all fleets when {@code fleetId} is the
     * all-fleets sentinel).
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to, or the all-fleets sentinel for every airframe
     * @return the matching airframe name/id pairs
     * @throws SQLException if the query fails
     */
    public static AirframeNameID[] getAllWithIds(Connection connection, int fleetId) throws SQLException {

        ArrayList<AirframeNameID> airframes = new ArrayList<>();
        String queryString;

        // Get all airframes regardless of fleet
        if (fleetId == FLEET_ID_ALL) {

            LOG.info("Getting airframe name ID pairs regardless of fleet");

            queryString = """
                SELECT
                    airframe, id
                FROM
                    airframes
                ORDER BY airframe
            """;

            try (PreparedStatement query = connection.prepareStatement(queryString)) {

                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        // airframe existed in the database, return the id
                        String airframe = resultSet.getString(1);
                        int id = resultSet.getInt(2);

                        AirframeNameID airframeNameID = new AirframeNameID(airframe, id);
                        airframes.add(airframeNameID);
                    }
                }
            }

            // Get all airframes for a specific fleet
        } else {

            LOG.log(Level.INFO, "Getting airframe name ID pairs for fleet: {0}", fleetId);

            queryString = """
                SELECT
                    airframe, id
                FROM
                    airframes
                INNER JOIN
                    fleet_airframes ON airframes.id = fleet_airframes.airframe_id
                WHERE
                    fleet_airframes.fleet_id = ?
                ORDER BY
                    airframe
            """;

            try (PreparedStatement query = connection.prepareStatement(queryString)) {
                query.setInt(1, fleetId);

                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        // airframe existed in the database, return the id
                        String airframe = resultSet.getString(1);
                        int id = resultSet.getInt(2);

                        AirframeNameID airframeNameID = new AirframeNameID(airframe, id);
                        airframes.add(airframeNameID);
                    }
                }
            }
        }

        return airframes.toArray(AirframeNameID[]::new);
    }

    /**
     * Returns the names of all airframes in the database (across all fleets), ordered by name.
     *
     * @param connection the database connection
     * @return every airframe name
     * @throws SQLException if the query fails
     */
    public static ArrayList<String> getAll(Connection connection) throws SQLException {

        String queryString = "SELECT airframe FROM airframes ORDER BY airframe";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            ArrayList<String> airframes = new ArrayList<>();

            while (resultSet.next()) {
                // airframe existed in the database, return the id
                String airframe = resultSet.getString(1);
                airframes.add(airframe);
            }

            return airframes;
        }
    }

    /**
     * Queries the database for all airframe names and their ids and returns a
     * hashmap of them.
     *
     * @param connection is a connection to the database
     * @return a HashMap of all airframe name ids to their names
     */
    public static HashMap<Integer, String> getIdToNameMap(Connection connection) throws SQLException {

        String queryString = "SELECT id, airframe FROM airframes ORDER BY id";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            HashMap<Integer, String> idToNameMap = new HashMap<Integer, String>();

            while (resultSet.next()) {
                // airframe existed in the database, return the id
                int id = resultSet.getInt(1);
                String airframe = resultSet.getString(2);
                idToNameMap.put(id, airframe);
            }

            return idToNameMap;
        }
    }

    /**
     * Queries the database for all airframe names and their ids and returns a
     * hashmap of them.
     *
     * @param connection is a connection to the database
     * @param fleetId    is the id of the fleet for the airframes
     * @return a HashMap of all airframe name ids to their names
     */
    public static HashMap<Integer, String> getIdToNameMap(Connection connection, int fleetId) throws SQLException {
        HashMap<Integer, String> idToNameMap = new HashMap<Integer, String>();

        String queryString = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON "
                + "airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = "
                + fleetId + " ORDER BY airframe";

        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {

            while (resultSet.next()) {
                // airframe existed in the database, return the id
                int id = resultSet.getInt(1);
                String airframe = resultSet.getString(2);
                idToNameMap.put(id, airframe);
            }

            return idToNameMap;
        }
    }
}
