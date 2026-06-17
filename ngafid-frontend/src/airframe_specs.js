import 'bootstrap';

import React from "react";
import {createRoot} from 'react-dom/client';

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

const ROWS_PER_PAGE_OPTIONS = [10, 25, 50];
const DEFAULT_PAGE_SIZE = 10;

const SPEC_FIELDS = [
    {key: "ownerFleetId", label: "Owner Fleet", type: "readonly"},
    {key: "isPublic", label: "Access", type: "access"},
    {key: "sharedFleetIds", label: "Shared Fleet IDs", type: "fleetIds"},
    {key: "airframeId", label: "NGAFID Airframe ID", type: "number"},
    {key: "manufacturer", label: "Manufacturer", type: "text", required: true},
    {key: "model", label: "Model", type: "text", required: true},
    {key: "series", label: "Series", type: "text"},
    {key: "year", label: "Year", type: "number"},
    {key: "usageType", label: "Usage", type: "text"},
    {key: "helicopterType", label: "Helicopter Type", type: "text"},
    {key: "seats", label: "Seats", type: "number"},
    {key: "landingGear", label: "Landing Gear", type: "text"},
    {key: "maxGrossWeightLbs", label: "Max Gross Wt (lbs)", type: "number"},
    {key: "minFlyingWeightLbs", label: "Min Flying Wt (lbs)", type: "number"},
    {key: "emptyWeightLbs", label: "Empty Wt (lbs)", type: "number"},
    {key: "vneKias", label: "Vne (KIAS)", type: "text"},
    {key: "vmaKias", label: "Vma (KIAS)", type: "text"},
    {key: "vgKias", label: "Vg (KIAS)", type: "text"},
    {key: "vyKias", label: "Vy (KIAS)", type: "text"},
    {key: "vtoKias", label: "Vto (KIAS)", type: "text"},
    {key: "vcKias", label: "Vc (KIAS)", type: "text"},
    {key: "vappKias", label: "Vapp (KIAS)", type: "text"},
    {key: "vautoKias", label: "Vauto (KIAS)", type: "text"},
    {key: "vturbKias", label: "Vturb (KIAS)", type: "text"},
    {key: "vloKias", label: "Vlo (KIAS)", type: "text"},
    {key: "vleKias", label: "Vle (KIAS)", type: "text"},
    {key: "vtdKias", label: "Vtd (KIAS)", type: "text"},
    {key: "mrType", label: "MR Type", type: "text"},
    {key: "mrNumberBlades", label: "MR Blades", type: "number"},
    {key: "mrDiameterIn", label: "MR Diameter (in)", type: "number"},
    {key: "mrInboardBladeChordIn", label: "MR Inboard Chord", type: "number"},
    {key: "mrOutboardBladeChordIn", label: "MR Outboard Chord", type: "number"},
    {key: "mrBladeTwistDeg", label: "MR Twist (deg)", type: "number"},
    {key: "mrTipSpeed102pctRpmFps", label: "MR Tip Speed", type: "number"},
    {key: "mrAirfoil", label: "MR Airfoil", type: "text"},
    {key: "mrPowerOnMaxContinuousPct", label: "MR PO Max %", type: "text"},
    {key: "mrPowerOnMaxContinuousRpm", label: "MR PO Max RPM", type: "number"},
    {key: "mrPowerOnMinContinuousPct", label: "MR PO Min %", type: "text"},
    {key: "mrPowerOnMinContinuousRpm", label: "MR PO Min RPM", type: "number"},
    {key: "mrPowerOffMaxContinuousPct", label: "MR POff Max %", type: "text"},
    {key: "mrPowerOffMaxContinuousRpm", label: "MR POff Max RPM", type: "number"},
    {key: "mrPowerOffMinContinuousPct", label: "MR POff Min %", type: "text"},
    {key: "mrHubMaterial", label: "MR Hub Material", type: "text"},
    {key: "mrBladeMaterial", label: "MR Blade Material", type: "text"},
    {key: "mrBladeAreaFt2", label: "MR Blade Area", type: "number"},
    {key: "mrDiskAreaFt2", label: "MR Disk Area", type: "number"},
    {key: "trType", label: "TR Type", type: "text"},
    {key: "trNumberBlades", label: "TR Blades", type: "number"},
    {key: "trDiameterIn", label: "TR Diameter (in)", type: "number"},
    {key: "trPowerOnMaxContinuousRpm", label: "TR PO Max RPM", type: "number"},
    {key: "trBladeChordIn", label: "TR Chord (in)", type: "number"},
    {key: "trBladeTwistDeg", label: "TR Twist (deg)", type: "number"},
    {key: "trTipSpeed102pctRpmFps", label: "TR Tip Speed", type: "number"},
    {key: "distanceMrToTrIn", label: "MR-TR Distance", type: "number"},
    {key: "trBladeMaterial", label: "TR Blade Material", type: "text"},
    {key: "trBladeAreaFt2", label: "TR Blade Area", type: "number"},
    {key: "trDiskAreaFt2", label: "TR Disk Area", type: "number"},
    {key: "spanIn", label: "Span (in)", type: "number"},
    {key: "areaFt2", label: "Area (ft2)", type: "number"},
    {key: "engineMade", label: "Engine Made", type: "text"},
    {key: "engineModel", label: "Engine Model", type: "text"},
    {key: "engineNumber", label: "Engine Count", type: "number"},
    {key: "takeoffPower", label: "Takeoff Power", type: "number"},
    {key: "manufacturersRatingShp", label: "Mfr Rating SHP", type: "number"},
    {key: "maxContinuousRatingShp", label: "Max Cont SHP", type: "number"},
    {key: "maxFuelPressurePsi", label: "Max Fuel PSI", type: "number"},
    {key: "turbineOutletTempAeoMaxContinuousC", label: "TOT Max C", type: "number"},
    {key: "maxOperationalPressureAltitudeFt", label: "Max Op Alt (ft)", type: "number"},
    {key: "maxPressureAltitudeTakeoffLandingFt", label: "Max T/O Alt (ft)", type: "number"},
    {key: "minSlOperationAirTempC", label: "Min SL Temp C", type: "number"},
    {key: "maxSlOperationAirTempC", label: "Max SL Temp C", type: "number"},
    {key: "takeoffEpndb", label: "Takeoff EPNdB", type: "number"},
    {key: "flyoverEpndb", label: "Flyover EPNdB", type: "number"},
    {key: "approachEpndb", label: "Approach EPNdB", type: "number"},
    {key: "lbPerShp", label: "lb/shp", type: "number"},
    {key: "lbPerFt2", label: "lb/ft2", type: "number"},
];

const ACTIONS_WIDTH_PX = 90;

const STICKY_FIELD_KEYS = [
    "ownerFleetId",
    "isPublic",
    "sharedFleetIds",
    "airframeId",
    "manufacturer",
    "model",
    "series",
];

const COLUMN_WIDTH_PX = {
    ownerFleetId: 90,
    isPublic: 175,
    sharedFleetIds: 165,
    airframeId: 185,
    manufacturer: 135,
    model: 135,
    series: 115,
};

function isStickyField(fieldKey) {
    return STICKY_FIELD_KEYS.includes(fieldKey);
}

function getStickyLeftPx(fieldKey) {
    if (!isStickyField(fieldKey)) {
        return null;
    }
    let left = ACTIONS_WIDTH_PX;
    for (const key of STICKY_FIELD_KEYS) {
        if (key === fieldKey) {
            break;
        }
        left += COLUMN_WIDTH_PX[key];
    }
    return left;
}

function columnCellStyle(field, backgroundColor, zIndex = 1) {
    const width = COLUMN_WIDTH_PX[field.key] || 120;
    const allowOverflow = field.type === "access" || field.type === "fleetIds";
    const style = {
        minWidth: `${width}px`,
        maxWidth: `${width}px`,
        width: `${width}px`,
        whiteSpace: "nowrap",
        overflow: allowOverflow ? "visible" : "hidden",
        textOverflow: allowOverflow ? "clip" : "ellipsis",
        boxSizing: "border-box",
    };
    const left = getStickyLeftPx(field.key);
    if (left !== null) {
        style.position = "sticky";
        style.left = `${left}px`;
        style.zIndex = zIndex;
        style.backgroundColor = backgroundColor;
    }
    return style;
}

function columnHeaderStyle(field, backgroundColor) {
    const style = columnCellStyle(field, backgroundColor, 3);
    style.overflow = "visible";
    style.textOverflow = "clip";
    style.whiteSpace = "normal";
    style.lineHeight = "1.2";
    style.paddingTop = "0.5rem";
    style.paddingBottom = "0.5rem";
    return style;
}

function parseFleetIdsString(raw) {
    if (Array.isArray(raw)) {
        return raw;
    }
    if (!raw || typeof raw !== "string") {
        return [];
    }
    return raw.split(",")
        .map((part) => part.trim())
        .filter((part) => part.length > 0)
        .map((part) => Number(part))
        .filter((num) => !Number.isNaN(num));
}

function emptySpec() {
    return {
        id: null,
        manufacturer: "",
        model: "",
        series: "",
        isPublic: true,
        sharedFleetIds: [],
        airframeId: null,
    };
}

function cloneSpec(spec) {
    return JSON.parse(JSON.stringify(spec));
}

function specPayloadForSave(spec) {
    const payload = cloneSpec(spec);
    delete payload.canEdit;
    delete payload.canManageSharing;
    if (payload.isPublic) {
        payload.sharedFleetIds = [];
    }
    return payload;
}

function parseFieldValue(field, raw) {
    if (field.type === "boolean") {
        return !!raw;
    }
    if (field.type === "number") {
        if (raw === "" || raw === null || raw === undefined) {
            return null;
        }
        const num = Number(raw);
        return Number.isNaN(num) ? null : num;
    }
    if (field.type === "fleetIds") {
        return parseFleetIdsString(raw);
    }
    return raw;
}

function formatDisplayValue(field, value) {
    if (field.type === "access") {
        return value ? "Public" : "Private";
    }
    if (field.type === "boolean") {
        return value ? "Yes" : "No";
    }
    if (field.type === "fleetIds") {
        return (value || []).join(", ");
    }
    if (value === null || value === undefined) {
        return "";
    }
    return String(value);
}

class AirframeSpecsPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            specs: [],
            newSpec: emptySpec(),
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount,
            loading: true,
            dirtyIds: {},
            currentPage: 0,
            pageSize: DEFAULT_PAGE_SIZE,
            totalCount: 0,
            showAddSpecPanel: false,
            fleetIdsText: {},
        };
    }

    componentDidMount() {
        this.loadSpecs(0);
    }

    toggleAddSpecPanel() {
        this.setState({showAddSpecPanel: !this.state.showAddSpecPanel});
    }

    getTotalPages() {
        return Math.max(1, Math.ceil(this.state.totalCount / this.state.pageSize));
    }

    loadSpecs(page = this.state.currentPage) {
        const {pageSize} = this.state;
        this.setState({loading: true});
        fetch(`/api/aircraft/rotorcraft-airframe-specs?page=${page}&pageSize=${pageSize}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Failed to load specs (${response.status})`);
                }
                return response.json();
            })
            .then((result) => {
                const specs = result.specs || [];
                const totalCount = result.total ?? specs.length;
                const currentPage = result.page ?? page;
                this.setState({
                    specs,
                    totalCount,
                    currentPage,
                    loading: false,
                    dirtyIds: {},
                    fleetIdsText: {},
                });
            })
            .catch((error) => {
                this.setState({loading: false});
                showErrorModal("Error Loading Airframe Specs", error.message);
            });
    }

    goToPage(page) {
        const totalPages = this.getTotalPages();
        const nextPage = Math.max(0, Math.min(page, totalPages - 1));
        if (nextPage === this.state.currentPage) {
            return;
        }
        this.loadSpecs(nextPage);
    }

    changePageSize(pageSize) {
        this.setState({pageSize, currentPage: 0}, () => this.loadSpecs(0));
    }

    markDirty(specId) {
        this.setState((prevState) => ({
            dirtyIds: {...prevState.dirtyIds, [specId]: true},
        }));
    }

    updateSpecField(specId, field, rawValue) {
        const specs = this.state.specs.map((spec) => {
            if (spec.id !== specId) {
                return spec;
            }
            const updated = cloneSpec(spec);
            updated[field.key] = parseFieldValue(field, rawValue);
            return updated;
        });
        this.setState({specs, dirtyIds: {...this.state.dirtyIds, [specId]: true}});
    }

    getFleetIdsText(draftKey, field, value) {
        if (this.state.fleetIdsText[draftKey] !== undefined) {
            return this.state.fleetIdsText[draftKey];
        }
        return formatDisplayValue(field, value);
    }

    updateFleetIdsField(draftKey, specId, field, rawValue) {
        const parsed = parseFleetIdsString(rawValue);
        if (draftKey === "new") {
            const newSpec = cloneSpec(this.state.newSpec);
            newSpec[field.key] = parsed;
            this.setState({
                newSpec,
                fleetIdsText: {...this.state.fleetIdsText, [draftKey]: rawValue},
            });
            return;
        }
        const specs = this.state.specs.map((spec) => {
            if (spec.id !== specId) {
                return spec;
            }
            const updated = cloneSpec(spec);
            updated[field.key] = parsed;
            return updated;
        });
        this.setState({
            specs,
            fleetIdsText: {...this.state.fleetIdsText, [draftKey]: rawValue},
            dirtyIds: {...this.state.dirtyIds, [specId]: true},
        });
    }

    clearFleetIdsText(draftKey) {
        if (this.state.fleetIdsText[draftKey] === undefined) {
            return;
        }
        const fleetIdsText = {...this.state.fleetIdsText};
        delete fleetIdsText[draftKey];
        this.setState({fleetIdsText});
    }

    updateNewSpecField(field, rawValue) {
        const newSpec = cloneSpec(this.state.newSpec);
        newSpec[field.key] = parseFieldValue(field, rawValue);
        this.setState({newSpec});
    }

    saveSpec(spec) {
        fetch(`/api/aircraft/rotorcraft-airframe-specs/${spec.id}`, {
            method: "PATCH",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(specPayloadForSave(spec)),
        })
            .then((response) => {
                if (!response.ok) {
                    return response.text().then((text) => {
                        throw new Error(text || `Save failed (${response.status})`);
                    });
                }
                return response.json();
            })
            .then((saved) => {
                const specs = this.state.specs.map((row) => (row.id === saved.id ? saved : row));
                const dirtyIds = {...this.state.dirtyIds};
                delete dirtyIds[saved.id];
                const fleetIdsText = {...this.state.fleetIdsText};
                delete fleetIdsText[String(saved.id)];
                this.setState({specs, dirtyIds, fleetIdsText});
            })
            .catch((error) => {
                showErrorModal("Error Saving Airframe Spec", error.message);
            });
    }

    createSpec() {
        const payload = specPayloadForSave(this.state.newSpec);
        if (!payload.manufacturer || !payload.model) {
            showErrorModal("Validation Error", "Manufacturer and model are required.");
            return;
        }
        fetch("/api/aircraft/rotorcraft-airframe-specs", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload),
        })
            .then((response) => {
                if (!response.ok) {
                    return response.text().then((text) => {
                        throw new Error(text || `Create failed (${response.status})`);
                    });
                }
                return response.json();
            })
            .then(() => {
                this.setState({
                    newSpec: emptySpec(),
                    showAddSpecPanel: false,
                }, () => this.loadSpecs(0));
            })
            .catch((error) => {
                showErrorModal("Error Creating Airframe Spec", error.message);
            });
    }

    renderAccessToggle(isPublic, onChange, disabled) {
        const activeStyle = {
            backgroundColor: "var(--c_confirm)",
            borderColor: "var(--c_confirm)",
            color: "white",
        };
        const inactiveStyle = {
            backgroundColor: "var(--c_row_bg)",
            borderColor: "var(--c_border, #adb5bd)",
            color: "var(--c_text)",
        };

        if (disabled) {
            return (
                <span
                    className="badge"
                    style={{
                        fontSize: "0.85rem",
                        backgroundColor: isPublic ? "var(--c_confirm)" : "var(--c_border, #6c757d)",
                        color: "white",
                    }}
                >
                    {isPublic ? "Public" : "Private"}
                </span>
            );
        }

        return (
            <div
                className="btn-group btn-group-sm"
                role="group"
                aria-label="Access visibility"
                style={{whiteSpace: "nowrap"}}
            >
                <button
                    type="button"
                    className="btn"
                    style={!isPublic ? activeStyle : inactiveStyle}
                    onClick={() => onChange(false)}
                >
                    Private
                </button>
                <button
                    type="button"
                    className="btn"
                    style={isPublic ? activeStyle : inactiveStyle}
                    onClick={() => onChange(true)}
                >
                    Public
                </button>
            </div>
        );
    }

    renderFieldInput(field, value, onChange, disabled, options = {}) {
        if (field.type === "readonly") {
            return <span>{formatDisplayValue(field, value)}</span>;
        }
        if (field.type === "access") {
            return this.renderAccessToggle(!!value, onChange, disabled);
        }
        if (field.type === "boolean") {
            return (
                <input
                    type="checkbox"
                    className="form-check-input"
                    checked={!!value}
                    disabled={disabled}
                    onChange={(event) => onChange(event.target.checked)}
                />
            );
        }
        if (field.type === "fleetIds") {
            return (
                <input
                    type="text"
                    className="form-control form-control-sm"
                    value={options.textValue ?? formatDisplayValue(field, value)}
                    placeholder="e.g. 3, 5, 7"
                    disabled={disabled}
                    onChange={(event) => onChange(event.target.value)}
                    onBlur={options.onBlur}
                />
            );
        }
        const inputType = field.type === "number" ? "number" : "text";
        const displayValue = value ?? "";
        return (
            <input
                type={inputType}
                className="form-control form-control-sm"
                value={displayValue}
                disabled={disabled}
                onChange={(event) => onChange(event.target.value)}
            />
        );
    }

    renderCollapsibleHeader(title, subtitle, expanded, onToggle) {
        return (
            <button
                type="button"
                className="card-header d-flex justify-content-between align-items-center w-100 text-left"
                onClick={onToggle}
                style={{
                    cursor: "pointer",
                    border: "none",
                    backgroundColor: "var(--c_navbar_bg)",
                    color: "var(--c_text)",
                }}
            >
                <div>
                    <strong>{title}</strong>
                    {subtitle && <div className="small text-muted mt-1">{subtitle}</div>}
                </div>
                <i className={`fa fa-chevron-${expanded ? "up" : "down"} ml-2`}/>
            </button>
        );
    }

    renderNewSpecCard() {
        const {showAddSpecPanel} = this.state;
        return (
            <div
                className="card m-0 border-top"
                style={{
                    flex: "0 0 auto",
                    borderRadius: 0,
                    backgroundColor: "var(--c_row_bg)",
                }}
            >
                {this.renderCollapsibleHeader(
                    "Add Airframe Spec",
                    "Expand to enter POH limit parameters for a new row",
                    showAddSpecPanel,
                    () => this.toggleAddSpecPanel(),
                )}
                {showAddSpecPanel && (
                    <div className="card-body">
                        <p className="mb-2" style={{color: "var(--c_text)"}}>
                            New rows are owned by your fleet. Public specs are visible to all fleets; private specs can
                            grant access via shared fleet IDs.
                        </p>
                        <div className="row">
                            {SPEC_FIELDS.filter((field) => field.type !== "readonly").map((field) => (
                                <div
                                    className={field.type === "access" ? "col-md-4 col-sm-12 mb-2" : "col-md-3 col-sm-6 mb-2"}
                                    key={`new-${field.key}`}
                                >
                                    <label className="small mb-1">{field.label}</label>
                                    {field.type === "access" && (
                                        <div className="small text-muted mb-1">
                                            Public specs are visible to all fleets.
                                        </div>
                                    )}
                                    {field.type === "fleetIds" && (
                                        <div className="small text-muted mb-1">
                                            Comma-separated fleet IDs, e.g. 3, 5, 7
                                        </div>
                                    )}
                                    {this.renderFieldInput(
                                        field,
                                        this.state.newSpec[field.key],
                                        (value) => (field.type === "fleetIds"
                                            ? this.updateFleetIdsField("new", null, field, value)
                                            : this.updateNewSpecField(field, value)),
                                        false,
                                        field.type === "fleetIds" ? {
                                            textValue: this.getFleetIdsText("new", field, this.state.newSpec[field.key]),
                                            onBlur: () => this.clearFleetIdsText("new"),
                                        } : {},
                                    )}
                                </div>
                            ))}
                        </div>
                        <button
                            type="button"
                            className="btn btn-primary mt-2"
                            style={{backgroundColor: "var(--c_confirm)", borderColor: "var(--c_confirm)"}}
                            onClick={() => this.createSpec()}
                        >
                            <i className="fa fa-plus"/> Add Spec
                        </button>
                    </div>
                )}
            </div>
        );
    }

    renderPagination() {
        const {currentPage, pageSize, totalCount} = this.state;
        const totalPages = this.getTotalPages();
        const startRow = totalCount === 0 ? 0 : currentPage * pageSize + 1;
        const endRow = Math.min(totalCount, (currentPage + 1) * pageSize);

        return (
            <div className="d-flex flex-wrap align-items-center justify-content-between m-2 mb-3">
                <div className="d-flex align-items-center mb-2 mb-md-0">
                    <span className="mr-2" style={{color: "var(--c_text)"}}>Rows per page:</span>
                    <select
                        className="form-control form-control-sm"
                        style={{width: "80px"}}
                        value={pageSize}
                        onChange={(event) => this.changePageSize(Number(event.target.value))}
                    >
                        {ROWS_PER_PAGE_OPTIONS.map((size) => (
                            <option key={size} value={size}>{size}</option>
                        ))}
                    </select>
                    <span className="ml-3 small" style={{color: "var(--c_text)"}}>
                        {startRow}–{endRow} of {totalCount}
                    </span>
                </div>
                <div className="btn-group">
                    <button
                        type="button"
                        className="btn btn-sm btn-outline-secondary"
                        disabled={currentPage <= 0}
                        onClick={() => this.goToPage(0)}
                        title="First page"
                    >
                        <i className="fa fa-angle-double-left"/>
                    </button>
                    <button
                        type="button"
                        className="btn btn-sm btn-outline-secondary"
                        disabled={currentPage <= 0}
                        onClick={() => this.goToPage(currentPage - 1)}
                        title="Previous page"
                    >
                        <i className="fa fa-angle-left"/>
                    </button>
                    <span
                        className="btn btn-sm btn-outline-secondary disabled"
                        style={{pointerEvents: "none", color: "var(--c_text)"}}
                    >
                        Page {currentPage + 1} of {totalPages}
                    </span>
                    <button
                        type="button"
                        className="btn btn-sm btn-outline-secondary"
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => this.goToPage(currentPage + 1)}
                        title="Next page"
                    >
                        <i className="fa fa-angle-right"/>
                    </button>
                    <button
                        type="button"
                        className="btn btn-sm btn-outline-secondary"
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => this.goToPage(totalPages - 1)}
                        title="Last page"
                    >
                        <i className="fa fa-angle-double-right"/>
                    </button>
                </div>
            </div>
        );
    }

    renderTable() {
        if (this.state.loading) {
            return <div className="m-3">Loading airframe specs...</div>;
        }

        return (
            <div>
                {this.renderPagination()}
                <div
                    className="m-2"
                    style={{
                        overflowX: "auto",
                        border: "1px solid var(--c_border, #ccc)",
                        backgroundColor: "var(--c_row_bg)",
                    }}
                >
                <table className="table table-sm table-bordered mb-0" style={{minWidth: "3600px"}}>
                    <thead style={{position: "sticky", top: 0, zIndex: 2, backgroundColor: "var(--c_navbar_bg)"}}>
                        <tr>
                            <th style={{
                                position: "sticky",
                                left: 0,
                                zIndex: 3,
                                backgroundColor: "var(--c_navbar_bg)",
                                minWidth: `${ACTIONS_WIDTH_PX}px`,
                                maxWidth: `${ACTIONS_WIDTH_PX}px`,
                                width: `${ACTIONS_WIDTH_PX}px`,
                            }}>
                                Actions
                            </th>
                            {SPEC_FIELDS.map((field) => (
                                <th
                                    key={field.key}
                                    style={columnHeaderStyle(field, "var(--c_navbar_bg)")}
                                    title={field.label}
                                >
                                    {field.label}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {this.state.specs.map((spec) => {
                            const editable = !!spec.canEdit;
                            const canManageSharing = !!spec.canManageSharing;
                            return (
                                <tr key={spec.id}>
                                    <td style={{
                                        position: "sticky",
                                        left: 0,
                                        zIndex: 1,
                                        backgroundColor: "var(--c_row_bg)",
                                        minWidth: `${ACTIONS_WIDTH_PX}px`,
                                        maxWidth: `${ACTIONS_WIDTH_PX}px`,
                                        width: `${ACTIONS_WIDTH_PX}px`,
                                    }}>
                                        {editable && this.state.dirtyIds[spec.id] && (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-primary"
                                                style={{
                                                    backgroundColor: "var(--c_confirm)",
                                                    borderColor: "var(--c_confirm)",
                                                }}
                                                onClick={() => this.saveSpec(spec)}
                                                title="Save changes"
                                            >
                                                <i className="fa fa-check"/>
                                            </button>
                                        )}
                                        {!editable && <span className="text-muted small">Read only</span>}
                                    </td>
                                    {SPEC_FIELDS.map((field) => {
                                        const fieldDisabled = !editable
                                            || (field.key === "isPublic" && !canManageSharing)
                                            || (field.key === "sharedFleetIds" && (!canManageSharing || spec.isPublic))
                                            || field.type === "readonly";
                                        const cellStyle = columnCellStyle(field, "var(--c_row_bg)");
                                        const draftKey = String(spec.id);
                                        return (
                                            <td key={`${spec.id}-${field.key}`} style={cellStyle}>
                                                {this.renderFieldInput(
                                                    field,
                                                    spec[field.key],
                                                    (value) => (field.type === "fleetIds"
                                                        ? this.updateFleetIdsField(draftKey, spec.id, field, value)
                                                        : this.updateSpecField(spec.id, field, value)),
                                                    fieldDisabled,
                                                    field.type === "fleetIds" ? {
                                                        textValue: this.getFleetIdsText(
                                                            draftKey,
                                                            field,
                                                            spec[field.key],
                                                        ),
                                                        onBlur: () => this.clearFleetIdsText(draftKey),
                                                    } : {},
                                                )}
                                            </td>
                                        );
                                    })}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
                </div>
                {this.renderPagination()}
            </div>
        );
    }

    render() {
        return (
            <div style={{display: "flex", flexDirection: "column", height: "100vh"}}>
                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar
                        activePage="account"
                        waitingUserCount={this.state.waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={this.state.unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div style={{overflowY: "auto", flex: "1 1 auto"}}>
                    <div className="container-fluid py-2 pb-0">
                        <h4 className="ml-2 mb-1" style={{color: "var(--c_text)"}}>Manage Airframe Specs</h4>
                        <p className="ml-2 mb-2" style={{color: "var(--c_text)"}}>
                            Browse POH limit parameters by page. Scroll horizontally for additional columns.
                            Rows are filtered by your fleet access (public, owned, or shared private specs).
                            You can edit rows owned by your fleet.
                        </p>
                        {this.renderTable()}
                    </div>
                </div>
                {this.renderNewSpecCard()}
            </div>
        );
    }
}

const container = document.querySelector("#airframe-specs-page");
const root = createRoot(container);
root.render(
    <AirframeSpecsPage
        waitingUserCount={waitingUserCount}
        unconfirmedTailsCount={unconfirmedTailsCount}
    />,
);
