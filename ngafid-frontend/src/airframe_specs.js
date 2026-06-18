import 'bootstrap';

import React from "react";
import {createRoot} from 'react-dom/client';

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

const ROWS_PER_PAGE_OPTIONS = [10, 25, 50];
const DEFAULT_PAGE_SIZE = 10;
const HAS_VIEW_ACCESS = typeof rotorcraftSpecsView !== "undefined" && rotorcraftSpecsView;
const HAS_EDIT_ACCESS = typeof rotorcraftSpecsEdit !== "undefined" && rotorcraftSpecsEdit;

const SPEC_FIELDS = [
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

const ACTIONS_WIDTH_PX = 100;

const STICKY_FIELD_KEYS = [
    "manufacturer",
    "model",
];

const COLUMN_WIDTH_PX = {
    manufacturer: 135,
    model: 135,
    series: 115,
};

function isStickyField(fieldKey) {
    return STICKY_FIELD_KEYS.includes(fieldKey);
}

function getStickyLeftPx(fieldKey, actionsWidth = ACTIONS_WIDTH_PX) {
    if (!isStickyField(fieldKey)) {
        return null;
    }
    let left = actionsWidth;
    for (const key of STICKY_FIELD_KEYS) {
        if (key === fieldKey) {
            break;
        }
        left += COLUMN_WIDTH_PX[key];
    }
    return left;
}

function columnCellStyle(field, backgroundColor, zIndex = 1, actionsWidth = ACTIONS_WIDTH_PX) {
    const width = COLUMN_WIDTH_PX[field.key] || 120;
    const style = {
        minWidth: `${width}px`,
        maxWidth: `${width}px`,
        width: `${width}px`,
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
        boxSizing: "border-box",
    };
    const left = getStickyLeftPx(field.key, actionsWidth);
    if (left !== null) {
        style.position = "sticky";
        style.left = `${left}px`;
        style.zIndex = zIndex;
        style.backgroundColor = backgroundColor;
    }
    return style;
}

function columnHeaderStyle(field, backgroundColor, actionsWidth = ACTIONS_WIDTH_PX) {
    const style = columnCellStyle(field, backgroundColor, 3, actionsWidth);
    style.overflow = "visible";
    style.textOverflow = "clip";
    style.whiteSpace = "normal";
    style.lineHeight = "1.2";
    style.paddingTop = "0.5rem";
    style.paddingBottom = "0.5rem";
    return style;
}

function emptySpec() {
    return {
        id: null,
        manufacturer: "",
        model: "",
        series: "",
    };
}

function cloneSpec(spec) {
    return JSON.parse(JSON.stringify(spec));
}

function specPayloadForSave(spec) {
    const payload = cloneSpec(spec);
    delete payload.canEdit;
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
    return raw;
}

class AirframeSpecsPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            specs: [],
            newSpec: emptySpec(),
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount,
            loading: HAS_VIEW_ACCESS,
            dirtyIds: {},
            editingIds: {},
            editSnapshots: {},
            currentPage: 0,
            pageSize: DEFAULT_PAGE_SIZE,
            totalCount: 0,
            canEdit: HAS_EDIT_ACCESS,
            showAddSpecPanel: false,
        };
    }

    componentDidMount() {
        if (HAS_VIEW_ACCESS) {
            this.loadSpecs(0);
        }
    }

    openAddSpecPanel() {
        this.setState({
            showAddSpecPanel: true,
            newSpec: emptySpec(),
        });
    }

    closeAddSpecPanel() {
        this.setState({
            showAddSpecPanel: false,
            newSpec: emptySpec(),
        });
    }

    getTotalPages() {
        return Math.max(1, Math.ceil(this.state.totalCount / this.state.pageSize));
    }

    loadSpecs(page = this.state.currentPage) {
        const {pageSize} = this.state;
        this.setState({loading: true});
        fetch(`/api/aircraft/rotorcraft-airframe-specs?page=${page}&pageSize=${pageSize}`)
            .then((response) => {
                if (response.status === 401) {
                    throw new Error("You do not have access to rotorcraft specs data. Request permissions from NGAFID admins.");
                }
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
                    canEdit: !!result.canEdit,
                    loading: false,
                    dirtyIds: {},
                    editingIds: {},
                    editSnapshots: {},
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

    startEditing(spec) {
        const specId = spec.id;
        this.setState((prevState) => ({
            editingIds: {...prevState.editingIds, [specId]: true},
            editSnapshots: {...prevState.editSnapshots, [specId]: cloneSpec(spec)},
        }));
    }

    cancelEditing(specId) {
        const snapshot = this.state.editSnapshots[specId];
        const specs = snapshot
            ? this.state.specs.map((row) => (row.id === specId ? snapshot : row))
            : this.state.specs;
        const editingIds = {...this.state.editingIds};
        const editSnapshots = {...this.state.editSnapshots};
        const dirtyIds = {...this.state.dirtyIds};
        delete editingIds[specId];
        delete editSnapshots[specId];
        delete dirtyIds[specId];
        this.setState({specs, editingIds, editSnapshots, dirtyIds});
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
                const editingIds = {...this.state.editingIds};
                const editSnapshots = {...this.state.editSnapshots};
                delete dirtyIds[saved.id];
                delete editingIds[saved.id];
                delete editSnapshots[saved.id];
                this.setState({specs, dirtyIds, editingIds, editSnapshots});
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
                this.closeAddSpecPanel();
                this.loadSpecs(0);
            })
            .catch((error) => {
                showErrorModal("Error Creating Airframe Spec", error.message);
            });
    }

    renderFieldInput(field, value, onChange, disabled) {
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
        const displayValue = value ?? "";
        return (
            <input
                type="text"
                className="form-control form-control-sm"
                inputMode={field.type === "number" ? "decimal" : undefined}
                value={displayValue}
                disabled={disabled}
                onChange={(event) => onChange(event.target.value)}
            />
        );
    }

    renderRowActions(spec, editable) {
        const specId = spec.id;
        const isEditing = !!this.state.editingIds[specId];
        const isDirty = !!this.state.dirtyIds[specId];
        const confirmBtnStyle = {
            backgroundColor: "var(--c_confirm)",
            borderColor: "var(--c_confirm)",
        };

        if (!editable) {
            return null;
        }
        if (!isEditing) {
            return (
                <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => this.startEditing(spec)}
                >
                    Edit
                </button>
            );
        }
        return (
            <div className="d-flex flex-column" style={{gap: "0.25rem"}}>
                {isDirty && (
                    <button
                        type="button"
                        className="btn btn-sm btn-primary"
                        style={confirmBtnStyle}
                        onClick={() => this.saveSpec(spec)}
                        title="Save changes"
                    >
                        Save
                    </button>
                )}
                <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => this.cancelEditing(specId)}
                    title={isDirty ? "Discard changes" : "Stop editing"}
                >
                    Cancel
                </button>
            </div>
        );
    }

    renderAddSpecButton() {
        if (!this.state.canEdit) {
            return null;
        }
        return (
            <div className="m-2 mb-3">
                <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    style={{backgroundColor: "var(--c_confirm)", borderColor: "var(--c_confirm)"}}
                    onClick={() => this.openAddSpecPanel()}
                >
                    <i className="fa fa-plus mr-1"/> Add Airframe Spec
                </button>
            </div>
        );
    }

    renderAddSpecForm() {
        return (
            <div
                className="m-2"
                style={{
                    border: "1px solid var(--c_border, #ccc)",
                    backgroundColor: "var(--c_row_bg)",
                    padding: "1rem",
                }}
            >
                <h5 className="mb-2" style={{color: "var(--c_text)"}}>Add Airframe Spec</h5>
                <p className="mb-3 small" style={{color: "var(--c_text)"}}>
                    Manufacturer and model are required. Series may be left blank.
                </p>
                <div className="row">
                    {SPEC_FIELDS.map((field) => (
                        <div className="col-md-3 col-sm-6 mb-2" key={`new-${field.key}`}>
                            <label className="small mb-1">{field.label}</label>
                            {this.renderFieldInput(
                                field,
                                this.state.newSpec[field.key],
                                (value) => this.updateNewSpecField(field, value),
                                false,
                            )}
                        </div>
                    ))}
                </div>
                <div className="mt-3">
                    <button
                        type="button"
                        className="btn btn-primary btn-sm mr-2"
                        style={{backgroundColor: "var(--c_confirm)", borderColor: "var(--c_confirm)"}}
                        onClick={() => this.createSpec()}
                    >
                        <i className="fa fa-check mr-1"/> Confirm
                    </button>
                    <button
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => this.closeAddSpecPanel()}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        );
    }

    renderNoAccess() {
        return (
            <div className="m-4 p-4" style={{
                border: "1px solid var(--c_border, #ccc)",
                backgroundColor: "var(--c_row_bg)",
                color: "var(--c_text)",
            }}>
                <h5 className="mb-2">Access denied</h5>
                <p className="mb-0">
                    You have no access to rotorcraft specs data. Request permissions from NGAFID admins.
                </p>
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

        const editable = this.state.canEdit;
        const stickyActionsWidth = editable ? ACTIONS_WIDTH_PX : 0;

        return (
            <div>
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
                            {editable && (
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
                            )}
                            {SPEC_FIELDS.map((field) => (
                                <th
                                    key={field.key}
                                    style={columnHeaderStyle(field, "var(--c_navbar_bg)", stickyActionsWidth)}
                                    title={field.label}
                                >
                                    {field.label}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {this.state.specs.map((spec) => {
                            const isEditing = !!this.state.editingIds[spec.id];
                            return (
                                <tr key={spec.id}>
                                    {editable && (
                                        <td style={{
                                            position: "sticky",
                                            left: 0,
                                            zIndex: 1,
                                            backgroundColor: "var(--c_row_bg)",
                                            minWidth: `${ACTIONS_WIDTH_PX}px`,
                                            maxWidth: `${ACTIONS_WIDTH_PX}px`,
                                            width: `${ACTIONS_WIDTH_PX}px`,
                                            verticalAlign: "middle",
                                        }}>
                                            {this.renderRowActions(spec, editable)}
                                        </td>
                                    )}
                                    {SPEC_FIELDS.map((field) => {
                                        const fieldDisabled = !editable || !isEditing;
                                        const cellStyle = columnCellStyle(field, "var(--c_row_bg)", 1, stickyActionsWidth);
                                        return (
                                            <td key={`${spec.id}-${field.key}`} style={cellStyle}>
                                                {this.renderFieldInput(
                                                    field,
                                                    spec[field.key],
                                                    (value) => this.updateSpecField(spec.id, field, value),
                                                    fieldDisabled,
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

    renderContent() {
        if (!HAS_VIEW_ACCESS) {
            return this.renderNoAccess();
        }
        if (this.state.showAddSpecPanel) {
            return this.renderAddSpecForm();
        }
        return (
            <>
                {this.renderAddSpecButton()}
                {this.renderTable()}
            </>
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
                        {HAS_VIEW_ACCESS && (
                            <p className="ml-2 mb-2" style={{color: "var(--c_text)"}}>
                                Browse POH limit parameters by page. Scroll horizontally for additional columns.
                                {this.state.canEdit
                                    ? " Click Edit to change a row, then Save."
                                    : " You have view-only access."}
                            </p>
                        )}
                        {this.renderContent()}
                    </div>
                </div>
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
