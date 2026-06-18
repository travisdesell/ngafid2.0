package org.ngafid.core.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CRUD for {@code rotorcraft_airframe_specs}. Access is controlled by per-user flags on {@code user},
 * not by fleet ownership.
 */
public final class RotorcraftAirframeSpecs {

    private RotorcraftAirframeSpecs() {}

    public static final class Spec {
        public int id;
        public Integer airframeId;
        public String manufacturer = "";
        public String model = "";
        public String series = "";
        public Integer year;
        public String usageType;
        public String helicopterType;
        public Integer seats;
        public String landingGear;
        public Double maxGrossWeightLbs;
        public Double minFlyingWeightLbs;
        public Double emptyWeightLbs;
        public String mrType;
        public Integer mrNumberBlades;
        public Double mrDiameterIn;
        public Double mrInboardBladeChordIn;
        public Double mrOutboardBladeChordIn;
        public Double mrBladeTwistDeg;
        public Double mrTipSpeed102pctRpmFps;
        public String mrAirfoil;
        public String mrPowerOnMaxContinuousPct;
        public Double mrPowerOnMaxContinuousRpm;
        public String mrPowerOnMinContinuousPct;
        public Double mrPowerOnMinContinuousRpm;
        public String mrPowerOffMaxContinuousPct;
        public Double mrPowerOffMaxContinuousRpm;
        public String mrPowerOffMinContinuousPct;
        public String mrHubMaterial;
        public String mrBladeMaterial;
        public Double mrBladeAreaFt2;
        public Double mrDiskAreaFt2;
        public String trType;
        public Integer trNumberBlades;
        public Double trDiameterIn;
        public Double trPowerOnMaxContinuousRpm;
        public Double trBladeChordIn;
        public Double trBladeTwistDeg;
        public Double trTipSpeed102pctRpmFps;
        public Double distanceMrToTrIn;
        public String trBladeMaterial;
        public Double trBladeAreaFt2;
        public Double trDiskAreaFt2;
        public Double spanIn;
        public Double areaFt2;
        public String vneKias;
        public String vmaKias;
        public String vgKias;
        public String vyKias;
        public String vtoKias;
        public String vcKias;
        public String vappKias;
        public String vautoKias;
        public String vturbKias;
        public String vloKias;
        public String vleKias;
        public String vtdKias;
        public String engineMade;
        public String engineModel;
        public Integer engineNumber;
        public Double takeoffPower;
        public Double manufacturersRatingShp;
        public Double maxContinuousRatingShp;
        public Double maxFuelPressurePsi;
        public Double turbineOutletTempAeoMaxContinuousC;
        public Double maxOperationalPressureAltitudeFt;
        public Double maxPressureAltitudeTakeoffLandingFt;
        public Double minSlOperationAirTempC;
        public Double maxSlOperationAirTempC;
        public Double takeoffEpndb;
        public Double flyoverEpndb;
        public Double approachEpndb;
        public Double lbPerShp;
        public Double lbPerFt2;

        /** UI hint: current user may edit rows. */
        public boolean canEdit;
    }

    private static final String SELECT_COLUMNS = """
            s.id, s.airframe_id,
            s.manufacturer, s.model, s.series, s.year, s.usage_type, s.helicopter_type, s.seats, s.landing_gear,
            s.max_gross_weight_lbs, s.min_flying_weight_lbs, s.empty_weight_lbs,
            s.mr_type, s.mr_number_blades, s.mr_diameter_in, s.mr_inboard_blade_chord_in, s.mr_outboard_blade_chord_in,
            s.mr_blade_twist_deg, s.mr_tip_speed_102pct_rpm_fps, s.mr_airfoil,
            s.mr_power_on_max_continuous_pct, s.mr_power_on_max_continuous_rpm, s.mr_power_on_min_continuous_pct,
            s.mr_power_on_min_continuous_rpm, s.mr_power_off_max_continuous_pct, s.mr_power_off_max_continuous_rpm,
            s.mr_power_off_min_continuous_pct, s.mr_hub_material, s.mr_blade_material, s.mr_blade_area_ft2, s.mr_disk_area_ft2,
            s.tr_type, s.tr_number_blades, s.tr_diameter_in, s.tr_power_on_max_continuous_rpm, s.tr_blade_chord_in,
            s.tr_blade_twist_deg, s.tr_tip_speed_102pct_rpm_fps, s.distance_mr_to_tr_in, s.tr_blade_material,
            s.tr_blade_area_ft2, s.tr_disk_area_ft2, s.span_in, s.area_ft2,
            s.vne_kias, s.vma_kias, s.vg_kias, s.vy_kias, s.vto_kias, s.vc_kias, s.vapp_kias, s.vauto_kias,
            s.vturb_kias, s.vlo_kias, s.vle_kias, s.vtd_kias,
            s.engine_made, s.engine_model, s.engine_number, s.takeoff_power, s.manufacturers_rating_shp,
            s.max_continuous_rating_shp, s.max_fuel_pressure_psi, s.turbine_outlet_temp_aeo_max_continuous_c,
            s.max_operational_pressure_altitude_ft, s.max_pressure_altitude_takeoff_landing_ft,
            s.min_sl_operation_air_temp_c, s.max_sl_operation_air_temp_c,
            s.takeoff_epndb, s.flyover_epndb, s.approach_epndb, s.lb_per_shp, s.lb_per_ft2
            """;

    public static final class Page {
        public int total;
        public int page;
        public int pageSize;
        public boolean canEdit;
        public List<Spec> specs = new ArrayList<>();
    }

    public static Page listPage(Connection connection, boolean userCanEdit, int page, int pageSize)
            throws SQLException {
        if (page < 0) {
            page = 0;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 50) {
            pageSize = 50;
        }

        Page result = new Page();
        result.page = page;
        result.pageSize = pageSize;
        result.canEdit = userCanEdit;

        try (PreparedStatement countQuery =
                connection.prepareStatement("SELECT COUNT(*) FROM rotorcraft_airframe_specs");
                ResultSet rs = countQuery.executeQuery()) {
            if (rs.next()) {
                result.total = rs.getInt(1);
            }
        }

        String sql = """
                SELECT %s
                FROM rotorcraft_airframe_specs s
                ORDER BY s.manufacturer, s.model, s.series, s.id
                LIMIT ? OFFSET ?
                """.formatted(SELECT_COLUMNS);

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, pageSize);
            query.setInt(2, page * pageSize);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) {
                    Spec spec = readSpec(rs);
                    spec.canEdit = userCanEdit;
                    result.specs.add(spec);
                }
            }
        }
        return result;
    }

    public static Spec getById(Connection connection, int specId, boolean userCanEdit) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM rotorcraft_airframe_specs s WHERE s.id = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, specId);
            try (ResultSet rs = query.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Spec spec = readSpec(rs);
                spec.canEdit = userCanEdit;
                return spec;
            }
        }
    }

    public static Spec insert(Connection connection, Spec spec) throws SQLException {
        Objects.requireNonNull(spec.manufacturer, "manufacturer");
        Objects.requireNonNull(spec.model, "model");
        if (spec.series == null) {
            spec.series = "";
        }

        String sql = """
                INSERT INTO rotorcraft_airframe_specs (
                    airframe_id,
                    manufacturer, model, series, year, usage_type, helicopter_type, seats, landing_gear,
                    max_gross_weight_lbs, min_flying_weight_lbs, empty_weight_lbs,
                    mr_type, mr_number_blades, mr_diameter_in, mr_inboard_blade_chord_in, mr_outboard_blade_chord_in,
                    mr_blade_twist_deg, mr_tip_speed_102pct_rpm_fps, mr_airfoil,
                    mr_power_on_max_continuous_pct, mr_power_on_max_continuous_rpm, mr_power_on_min_continuous_pct,
                    mr_power_on_min_continuous_rpm, mr_power_off_max_continuous_pct, mr_power_off_max_continuous_rpm,
                    mr_power_off_min_continuous_pct, mr_hub_material, mr_blade_material, mr_blade_area_ft2, mr_disk_area_ft2,
                    tr_type, tr_number_blades, tr_diameter_in, tr_power_on_max_continuous_rpm, tr_blade_chord_in,
                    tr_blade_twist_deg, tr_tip_speed_102pct_rpm_fps, distance_mr_to_tr_in, tr_blade_material,
                    tr_blade_area_ft2, tr_disk_area_ft2, span_in, area_ft2,
                    vne_kias, vma_kias, vg_kias, vy_kias, vto_kias, vc_kias, vapp_kias, vauto_kias,
                    vturb_kias, vlo_kias, vle_kias, vtd_kias,
                    engine_made, engine_model, engine_number, takeoff_power, manufacturers_rating_shp,
                    max_continuous_rating_shp, max_fuel_pressure_psi, turbine_outlet_temp_aeo_max_continuous_c,
                    max_operational_pressure_altitude_ft, max_pressure_altitude_takeoff_landing_ft,
                    min_sl_operation_air_temp_c, max_sl_operation_air_temp_c,
                    takeoff_epndb, flyover_epndb, approach_epndb, lb_per_shp, lb_per_ft2
                ) VALUES (
                    ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                """;

        try (PreparedStatement query = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            setInteger(query, i++, spec.airframeId);
            query.setString(i++, spec.manufacturer);
            query.setString(i++, spec.model);
            query.setString(i++, spec.series);
            setInteger(query, i++, spec.year);
            query.setString(i++, spec.usageType);
            query.setString(i++, spec.helicopterType);
            setInteger(query, i++, spec.seats);
            query.setString(i++, spec.landingGear);
            setDouble(query, i++, spec.maxGrossWeightLbs);
            setDouble(query, i++, spec.minFlyingWeightLbs);
            setDouble(query, i++, spec.emptyWeightLbs);
            query.setString(i++, spec.mrType);
            setInteger(query, i++, spec.mrNumberBlades);
            setDouble(query, i++, spec.mrDiameterIn);
            setDouble(query, i++, spec.mrInboardBladeChordIn);
            setDouble(query, i++, spec.mrOutboardBladeChordIn);
            setDouble(query, i++, spec.mrBladeTwistDeg);
            setDouble(query, i++, spec.mrTipSpeed102pctRpmFps);
            query.setString(i++, spec.mrAirfoil);
            query.setString(i++, spec.mrPowerOnMaxContinuousPct);
            setDouble(query, i++, spec.mrPowerOnMaxContinuousRpm);
            query.setString(i++, spec.mrPowerOnMinContinuousPct);
            setDouble(query, i++, spec.mrPowerOnMinContinuousRpm);
            query.setString(i++, spec.mrPowerOffMaxContinuousPct);
            setDouble(query, i++, spec.mrPowerOffMaxContinuousRpm);
            query.setString(i++, spec.mrPowerOffMinContinuousPct);
            query.setString(i++, spec.mrHubMaterial);
            query.setString(i++, spec.mrBladeMaterial);
            setDouble(query, i++, spec.mrBladeAreaFt2);
            setDouble(query, i++, spec.mrDiskAreaFt2);
            query.setString(i++, spec.trType);
            setInteger(query, i++, spec.trNumberBlades);
            setDouble(query, i++, spec.trDiameterIn);
            setDouble(query, i++, spec.trPowerOnMaxContinuousRpm);
            setDouble(query, i++, spec.trBladeChordIn);
            setDouble(query, i++, spec.trBladeTwistDeg);
            setDouble(query, i++, spec.trTipSpeed102pctRpmFps);
            setDouble(query, i++, spec.distanceMrToTrIn);
            query.setString(i++, spec.trBladeMaterial);
            setDouble(query, i++, spec.trBladeAreaFt2);
            setDouble(query, i++, spec.trDiskAreaFt2);
            setDouble(query, i++, spec.spanIn);
            setDouble(query, i++, spec.areaFt2);
            query.setString(i++, spec.vneKias);
            query.setString(i++, spec.vmaKias);
            query.setString(i++, spec.vgKias);
            query.setString(i++, spec.vyKias);
            query.setString(i++, spec.vtoKias);
            query.setString(i++, spec.vcKias);
            query.setString(i++, spec.vappKias);
            query.setString(i++, spec.vautoKias);
            query.setString(i++, spec.vturbKias);
            query.setString(i++, spec.vloKias);
            query.setString(i++, spec.vleKias);
            query.setString(i++, spec.vtdKias);
            query.setString(i++, spec.engineMade);
            query.setString(i++, spec.engineModel);
            setInteger(query, i++, spec.engineNumber);
            setDouble(query, i++, spec.takeoffPower);
            setDouble(query, i++, spec.manufacturersRatingShp);
            setDouble(query, i++, spec.maxContinuousRatingShp);
            setDouble(query, i++, spec.maxFuelPressurePsi);
            setDouble(query, i++, spec.turbineOutletTempAeoMaxContinuousC);
            setDouble(query, i++, spec.maxOperationalPressureAltitudeFt);
            setDouble(query, i++, spec.maxPressureAltitudeTakeoffLandingFt);
            setDouble(query, i++, spec.minSlOperationAirTempC);
            setDouble(query, i++, spec.maxSlOperationAirTempC);
            setDouble(query, i++, spec.takeoffEpndb);
            setDouble(query, i++, spec.flyoverEpndb);
            setDouble(query, i++, spec.approachEpndb);
            setDouble(query, i++, spec.lbPerShp);
            setDouble(query, i, spec.lbPerFt2);

            query.executeUpdate();
            try (ResultSet keys = query.getGeneratedKeys()) {
                if (keys.next()) {
                    spec.id = keys.getInt(1);
                }
            }
        }
        spec.canEdit = true;
        return spec;
    }

    public static Spec update(Connection connection, Spec spec, boolean userCanEdit) throws SQLException {
        if (!userCanEdit) {
            throw new SQLException("Not authorized to edit rotorcraft airframe specs.");
        }
        Spec existing = getById(connection, spec.id, true);
        if (existing == null) {
            throw new SQLException("Spec not found.");
        }
        if (spec.series == null) {
            spec.series = "";
        }

        String sql = """
                UPDATE rotorcraft_airframe_specs SET
                    airframe_id = ?,
                    manufacturer = ?, model = ?, series = ?, year = ?, usage_type = ?, helicopter_type = ?, seats = ?,
                    landing_gear = ?,
                    max_gross_weight_lbs = ?, min_flying_weight_lbs = ?, empty_weight_lbs = ?,
                    mr_type = ?, mr_number_blades = ?, mr_diameter_in = ?, mr_inboard_blade_chord_in = ?,
                    mr_outboard_blade_chord_in = ?, mr_blade_twist_deg = ?, mr_tip_speed_102pct_rpm_fps = ?, mr_airfoil = ?,
                    mr_power_on_max_continuous_pct = ?, mr_power_on_max_continuous_rpm = ?, mr_power_on_min_continuous_pct = ?,
                    mr_power_on_min_continuous_rpm = ?, mr_power_off_max_continuous_pct = ?, mr_power_off_max_continuous_rpm = ?,
                    mr_power_off_min_continuous_pct = ?, mr_hub_material = ?, mr_blade_material = ?, mr_blade_area_ft2 = ?,
                    mr_disk_area_ft2 = ?,
                    tr_type = ?, tr_number_blades = ?, tr_diameter_in = ?, tr_power_on_max_continuous_rpm = ?,
                    tr_blade_chord_in = ?, tr_blade_twist_deg = ?, tr_tip_speed_102pct_rpm_fps = ?, distance_mr_to_tr_in = ?,
                    tr_blade_material = ?, tr_blade_area_ft2 = ?, tr_disk_area_ft2 = ?, span_in = ?, area_ft2 = ?,
                    vne_kias = ?, vma_kias = ?, vg_kias = ?, vy_kias = ?, vto_kias = ?, vc_kias = ?, vapp_kias = ?,
                    vauto_kias = ?, vturb_kias = ?, vlo_kias = ?, vle_kias = ?, vtd_kias = ?,
                    engine_made = ?, engine_model = ?, engine_number = ?, takeoff_power = ?, manufacturers_rating_shp = ?,
                    max_continuous_rating_shp = ?, max_fuel_pressure_psi = ?, turbine_outlet_temp_aeo_max_continuous_c = ?,
                    max_operational_pressure_altitude_ft = ?, max_pressure_altitude_takeoff_landing_ft = ?,
                    min_sl_operation_air_temp_c = ?, max_sl_operation_air_temp_c = ?,
                    takeoff_epndb = ?, flyover_epndb = ?, approach_epndb = ?, lb_per_shp = ?, lb_per_ft2 = ?
                WHERE id = ?
                """;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            int i = 1;
            setInteger(query, i++, spec.airframeId);
            query.setString(i++, spec.manufacturer);
            query.setString(i++, spec.model);
            query.setString(i++, spec.series);
            setInteger(query, i++, spec.year);
            query.setString(i++, spec.usageType);
            query.setString(i++, spec.helicopterType);
            setInteger(query, i++, spec.seats);
            query.setString(i++, spec.landingGear);
            setDouble(query, i++, spec.maxGrossWeightLbs);
            setDouble(query, i++, spec.minFlyingWeightLbs);
            setDouble(query, i++, spec.emptyWeightLbs);
            query.setString(i++, spec.mrType);
            setInteger(query, i++, spec.mrNumberBlades);
            setDouble(query, i++, spec.mrDiameterIn);
            setDouble(query, i++, spec.mrInboardBladeChordIn);
            setDouble(query, i++, spec.mrOutboardBladeChordIn);
            setDouble(query, i++, spec.mrBladeTwistDeg);
            setDouble(query, i++, spec.mrTipSpeed102pctRpmFps);
            query.setString(i++, spec.mrAirfoil);
            query.setString(i++, spec.mrPowerOnMaxContinuousPct);
            setDouble(query, i++, spec.mrPowerOnMaxContinuousRpm);
            query.setString(i++, spec.mrPowerOnMinContinuousPct);
            setDouble(query, i++, spec.mrPowerOnMinContinuousRpm);
            query.setString(i++, spec.mrPowerOffMaxContinuousPct);
            setDouble(query, i++, spec.mrPowerOffMaxContinuousRpm);
            query.setString(i++, spec.mrPowerOffMinContinuousPct);
            query.setString(i++, spec.mrHubMaterial);
            query.setString(i++, spec.mrBladeMaterial);
            setDouble(query, i++, spec.mrBladeAreaFt2);
            setDouble(query, i++, spec.mrDiskAreaFt2);
            query.setString(i++, spec.trType);
            setInteger(query, i++, spec.trNumberBlades);
            setDouble(query, i++, spec.trDiameterIn);
            setDouble(query, i++, spec.trPowerOnMaxContinuousRpm);
            setDouble(query, i++, spec.trBladeChordIn);
            setDouble(query, i++, spec.trBladeTwistDeg);
            setDouble(query, i++, spec.trTipSpeed102pctRpmFps);
            setDouble(query, i++, spec.distanceMrToTrIn);
            query.setString(i++, spec.trBladeMaterial);
            setDouble(query, i++, spec.trBladeAreaFt2);
            setDouble(query, i++, spec.trDiskAreaFt2);
            setDouble(query, i++, spec.spanIn);
            setDouble(query, i++, spec.areaFt2);
            query.setString(i++, spec.vneKias);
            query.setString(i++, spec.vmaKias);
            query.setString(i++, spec.vgKias);
            query.setString(i++, spec.vyKias);
            query.setString(i++, spec.vtoKias);
            query.setString(i++, spec.vcKias);
            query.setString(i++, spec.vappKias);
            query.setString(i++, spec.vautoKias);
            query.setString(i++, spec.vturbKias);
            query.setString(i++, spec.vloKias);
            query.setString(i++, spec.vleKias);
            query.setString(i++, spec.vtdKias);
            query.setString(i++, spec.engineMade);
            query.setString(i++, spec.engineModel);
            setInteger(query, i++, spec.engineNumber);
            setDouble(query, i++, spec.takeoffPower);
            setDouble(query, i++, spec.manufacturersRatingShp);
            setDouble(query, i++, spec.maxContinuousRatingShp);
            setDouble(query, i++, spec.maxFuelPressurePsi);
            setDouble(query, i++, spec.turbineOutletTempAeoMaxContinuousC);
            setDouble(query, i++, spec.maxOperationalPressureAltitudeFt);
            setDouble(query, i++, spec.maxPressureAltitudeTakeoffLandingFt);
            setDouble(query, i++, spec.minSlOperationAirTempC);
            setDouble(query, i++, spec.maxSlOperationAirTempC);
            setDouble(query, i++, spec.takeoffEpndb);
            setDouble(query, i++, spec.flyoverEpndb);
            setDouble(query, i++, spec.approachEpndb);
            setDouble(query, i++, spec.lbPerShp);
            setDouble(query, i++, spec.lbPerFt2);
            query.setInt(i, spec.id);

            int updated = query.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Spec update failed.");
            }
        }

        spec.canEdit = true;
        return spec;
    }

    private static Spec readSpec(ResultSet rs) throws SQLException {
        Spec spec = new Spec();
        spec.id = rs.getInt("id");
        spec.airframeId = getInteger(rs, "airframe_id");
        spec.manufacturer = rs.getString("manufacturer");
        spec.model = rs.getString("model");
        spec.series = rs.getString("series");
        spec.year = getInteger(rs, "year");
        spec.usageType = rs.getString("usage_type");
        spec.helicopterType = rs.getString("helicopter_type");
        spec.seats = getInteger(rs, "seats");
        spec.landingGear = rs.getString("landing_gear");
        spec.maxGrossWeightLbs = getDouble(rs, "max_gross_weight_lbs");
        spec.minFlyingWeightLbs = getDouble(rs, "min_flying_weight_lbs");
        spec.emptyWeightLbs = getDouble(rs, "empty_weight_lbs");
        spec.mrType = rs.getString("mr_type");
        spec.mrNumberBlades = getInteger(rs, "mr_number_blades");
        spec.mrDiameterIn = getDouble(rs, "mr_diameter_in");
        spec.mrInboardBladeChordIn = getDouble(rs, "mr_inboard_blade_chord_in");
        spec.mrOutboardBladeChordIn = getDouble(rs, "mr_outboard_blade_chord_in");
        spec.mrBladeTwistDeg = getDouble(rs, "mr_blade_twist_deg");
        spec.mrTipSpeed102pctRpmFps = getDouble(rs, "mr_tip_speed_102pct_rpm_fps");
        spec.mrAirfoil = rs.getString("mr_airfoil");
        spec.mrPowerOnMaxContinuousPct = rs.getString("mr_power_on_max_continuous_pct");
        spec.mrPowerOnMaxContinuousRpm = getDouble(rs, "mr_power_on_max_continuous_rpm");
        spec.mrPowerOnMinContinuousPct = rs.getString("mr_power_on_min_continuous_pct");
        spec.mrPowerOnMinContinuousRpm = getDouble(rs, "mr_power_on_min_continuous_rpm");
        spec.mrPowerOffMaxContinuousPct = rs.getString("mr_power_off_max_continuous_pct");
        spec.mrPowerOffMaxContinuousRpm = getDouble(rs, "mr_power_off_max_continuous_rpm");
        spec.mrPowerOffMinContinuousPct = rs.getString("mr_power_off_min_continuous_pct");
        spec.mrHubMaterial = rs.getString("mr_hub_material");
        spec.mrBladeMaterial = rs.getString("mr_blade_material");
        spec.mrBladeAreaFt2 = getDouble(rs, "mr_blade_area_ft2");
        spec.mrDiskAreaFt2 = getDouble(rs, "mr_disk_area_ft2");
        spec.trType = rs.getString("tr_type");
        spec.trNumberBlades = getInteger(rs, "tr_number_blades");
        spec.trDiameterIn = getDouble(rs, "tr_diameter_in");
        spec.trPowerOnMaxContinuousRpm = getDouble(rs, "tr_power_on_max_continuous_rpm");
        spec.trBladeChordIn = getDouble(rs, "tr_blade_chord_in");
        spec.trBladeTwistDeg = getDouble(rs, "tr_blade_twist_deg");
        spec.trTipSpeed102pctRpmFps = getDouble(rs, "tr_tip_speed_102pct_rpm_fps");
        spec.distanceMrToTrIn = getDouble(rs, "distance_mr_to_tr_in");
        spec.trBladeMaterial = rs.getString("tr_blade_material");
        spec.trBladeAreaFt2 = getDouble(rs, "tr_blade_area_ft2");
        spec.trDiskAreaFt2 = getDouble(rs, "tr_disk_area_ft2");
        spec.spanIn = getDouble(rs, "span_in");
        spec.areaFt2 = getDouble(rs, "area_ft2");
        spec.vneKias = rs.getString("vne_kias");
        spec.vmaKias = rs.getString("vma_kias");
        spec.vgKias = rs.getString("vg_kias");
        spec.vyKias = rs.getString("vy_kias");
        spec.vtoKias = rs.getString("vto_kias");
        spec.vcKias = rs.getString("vc_kias");
        spec.vappKias = rs.getString("vapp_kias");
        spec.vautoKias = rs.getString("vauto_kias");
        spec.vturbKias = rs.getString("vturb_kias");
        spec.vloKias = rs.getString("vlo_kias");
        spec.vleKias = rs.getString("vle_kias");
        spec.vtdKias = rs.getString("vtd_kias");
        spec.engineMade = rs.getString("engine_made");
        spec.engineModel = rs.getString("engine_model");
        spec.engineNumber = getInteger(rs, "engine_number");
        spec.takeoffPower = getDouble(rs, "takeoff_power");
        spec.manufacturersRatingShp = getDouble(rs, "manufacturers_rating_shp");
        spec.maxContinuousRatingShp = getDouble(rs, "max_continuous_rating_shp");
        spec.maxFuelPressurePsi = getDouble(rs, "max_fuel_pressure_psi");
        spec.turbineOutletTempAeoMaxContinuousC = getDouble(rs, "turbine_outlet_temp_aeo_max_continuous_c");
        spec.maxOperationalPressureAltitudeFt = getDouble(rs, "max_operational_pressure_altitude_ft");
        spec.maxPressureAltitudeTakeoffLandingFt = getDouble(rs, "max_pressure_altitude_takeoff_landing_ft");
        spec.minSlOperationAirTempC = getDouble(rs, "min_sl_operation_air_temp_c");
        spec.maxSlOperationAirTempC = getDouble(rs, "max_sl_operation_air_temp_c");
        spec.takeoffEpndb = getDouble(rs, "takeoff_epndb");
        spec.flyoverEpndb = getDouble(rs, "flyover_epndb");
        spec.approachEpndb = getDouble(rs, "approach_epndb");
        spec.lbPerShp = getDouble(rs, "lb_per_shp");
        spec.lbPerFt2 = getDouble(rs, "lb_per_ft2");
        return spec;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Double getDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static void setInteger(PreparedStatement query, int index, Integer value) throws SQLException {
        if (value == null) {
            query.setNull(index, Types.INTEGER);
        } else {
            query.setInt(index, value);
        }
    }

    private static void setDouble(PreparedStatement query, int index, Double value) throws SQLException {
        if (value == null) {
            query.setNull(index, Types.DOUBLE);
        } else {
            query.setDouble(index, value);
        }
    }
}
