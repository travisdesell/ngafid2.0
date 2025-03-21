package org.ngafid.core.flights;

import org.ngafid.core.util.NormalizedColumn;
import org.ngafid.core.util.filters.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public enum Airframes {
    ;
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

    //CHECKSTYLE:OFF
    public static final Set<String> ROTORCRAFT = Set.of("R44", "Robinson R44");

    //CHECKSTYLE:ON
    public record AliasKey(String name, int fleetId) {
    }

    public static AliasKey defaultAlias(String name) {
        return new AliasKey(name, -1);
    }

    //CHECKSTYLE:OFF
    public static Map<AliasKey, String> AIRFRAME_ALIASES = Map.ofEntries(
            Map.entry(defaultAlias("Unknown Aircraft"), ""),
            Map.entry(defaultAlias("Garmin Flight Display"), ""),
            Map.entry(defaultAlias("Diamond DA 40"), "Diamond DA40"),
            Map.entry(new AliasKey("Garmin Flight Display", 1), "R44"),
            Map.entry(new AliasKey("Robinson R44 Raven I", 1), "R44"),
            Map.entry(defaultAlias("Robinson R44"), "R44"),
            Map.entry(defaultAlias("Cirrus SR22 (3600 GW)"), "Cirrus SR22"));

    //CHECKSTYLE:ON
    public static class Type extends NormalizedColumn<Type> {
        @Override
        protected String getTableName() {
            return "airframe_types";
        }

        public Type(String name) {
            super(name);
        }

        public Type(Connection connection, String name) throws SQLException {
            super(connection, name);
        }

        public Type(Connection connection, int id) throws SQLException {
            super(connection, id);
        }
    }

    public static class Airframe {
        private static final ConcurrentHashMap<String, Pair<Type, Integer>> NAME_TO_TYPE_AND_ID = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<Integer, Pair<Type, String>> ID_TO_TYPE_AND_NAME = new ConcurrentHashMap<>();

        private int id;
        private String name;
        private Type type;

        public static Airframe getAirframeByName(Connection connection, String airframeName) throws SQLException {
            return new Airframe(connection, airframeName, null);
        }

        public Airframe(String name, Type type) {
            this.id = -1;
            this.name = name;
            this.type = type;
        }

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
            try (PreparedStatement query = connection.prepareStatement("SELECT id, type_id FROM airframes WHERE airframe = ?")) {
                query.setString(1, name);

                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getInt("id");
                        this.type = new Type(connection, rs.getInt("type_id"));
                    } else {
                        if (type != null)
                            generateNewId(connection);
                        else
                            throw new SQLException("Airframe not found");
                    }
                }
            }
        }

        void generateNewId(Connection connection) throws SQLException {
            try (PreparedStatement query = connection.prepareStatement("INSERT IGNORE INTO airframes (airframe, type_id) VALUES (?, ?)")) {
                this.type = new Type(connection, this.type.getName());
                query.setString(1, name);
                query.setInt(2, type.getId());

                int rs = query.executeUpdate();
                getIdAndType(connection);
            }
        }

        private void getNameAndType(Connection connection) throws SQLException {
            try (PreparedStatement query = connection.prepareStatement("SELECT airframe, type_id FROM airframes WHERE id = ?")) {
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

    public static void setAirframeFleet(Connection connection, int airframeId, int fleetId) throws SQLException {
        String key = airframeId + "-" + fleetId;

        // this was already inserted to the database
        if (fleetAirframes.contains(key))
            return;
        else {
            String queryString = "REPLACE INTO fleet_airframes SET fleet_id = ?, airframe_id = ?";
            try (PreparedStatement query = connection.prepareStatement(queryString)) {
                query.setInt(1, fleetId);
                query.setInt(2, airframeId);

                // LOG.info(query.toString());
                query.executeUpdate();

                fleetAirframes.add(key);
            }
        }
    }

    public static ArrayList<String> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> airframes = new ArrayList<>();

        String queryString = "SELECT airframe FROM airframes INNER JOIN fleet_airframes ON " +
                "airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
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

        String queryString = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON " +
                "airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = "
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
