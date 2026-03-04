import 'bootstrap';
import React from "react";
import Button from 'react-bootstrap/Button';

/**
 * Format time for display: elapsed seconds -> "H:MM:SS", Unix timestamp -> datetime string.
 */
export function formatLabelingTime(time) {
    const t = toLabelingTimeNumber(time);
    if (t == null) return '—';
    if (t > 1e10) {
        return new Date(t * 1000).toISOString().slice(0, 19).replace('T', ' ');
    }
    const totalSeconds = Math.floor(t);
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    if (h > 0) {
        return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }
    if (m > 0) {
        return `${m}:${String(s).padStart(2, '0')}`;
    }
    return `${s}s`;
}

/**
 * Coerce time to number. Backend may send x as string indices (e.g. "2569") or numbers.
 * Treat as elapsed seconds from flight start, or as Unix timestamp if very large.
 */
function toLabelingTimeNumber(time) {
    if (time == null) return null;
    if (typeof time === 'number' && !isNaN(time)) return time;
    const n = Number(time);
    return isNaN(n) ? null : n;
}

/**
 * Get a Date for a point: from elapsed seconds + flight start, or Unix timestamp.
 * Time may come as string (e.g. backend sends x as string indices "0","1",...) or number.
 */
function getLabelingDate(time, startDateTime) {
    const t = toLabelingTimeNumber(time);
    if (t == null) return null;
    if (t > 1e10) return new Date(t * 1000);
    if (!startDateTime) return null;
    const start = new Date(String(startDateTime).replace(' ', 'T'));
    if (isNaN(start.getTime())) return null;
    return new Date(start.getTime() + t * 1000);
}

/**
 * Human-readable date and time for a point (single string).
 */
export function formatLabelingDateTime(time, startDateTime) {
    const d = getLabelingDate(time, startDateTime);
    if (!d) return formatLabelingTime(time);
    return d.toISOString().slice(0, 19).replace('T', ' ');
}

/**
 * Date only: YYYY-MM-DD
 */
export function formatLabelingDate(time, startDateTime) {
    const d = getLabelingDate(time, startDateTime);
    if (!d) return '—';
    return d.toISOString().slice(0, 10);
}

/**
 * Time only: HH:MM:SS
 */
export function formatLabelingTimeOnly(time, startDateTime) {
    const d = getLabelingDate(time, startDateTime);
    if (!d) return '—';
    return d.toISOString().slice(11, 19);
}

const LABELING_MARKER_COLORS = [
    '#e6194b', '#3cb44b', '#4363d8', '#f58231', '#911eb4', '#42d4f4', '#f032e6', '#bfef45',
    '#fabed4', '#469990', '#dcbeff', '#9a6324', '#fffac8', '#800000', '#aaffc3', '#808000', '#ffd8b1', '#000075',
];

const LABELING_POPUP_OFFSET = 12;
const LABELING_POPUP_WIDTH = 322; // default width for sections table
const LABELING_POPUP_HEIGHT = 320;
const LABELING_POPUP_MIN_WIDTH = 240;
const LABELING_POPUP_MAX_WIDTH = 600;
const LABELING_POPUP_MIN_HEIGHT = 200;
const LABELING_POPUP_MAX_HEIGHT = 800;
const LABELING_POPUP_WIDTH_STORAGE_KEY = 'ngafid_labeling_popup_width';
const LABELING_POPUP_HEIGHT_STORAGE_KEY = 'ngafid_labeling_popup_height';
const RESIZE_HANDLE_SIZE = 8;

function getStoredPopupWidth() {
    try {
        const stored = localStorage.getItem(LABELING_POPUP_WIDTH_STORAGE_KEY);
        if (stored == null) return LABELING_POPUP_WIDTH;
        const n = Number(stored);
        if (!Number.isFinite(n)) return LABELING_POPUP_WIDTH;
        return Math.max(LABELING_POPUP_MIN_WIDTH, Math.min(LABELING_POPUP_MAX_WIDTH, Math.round(n)));
    } catch {
        return LABELING_POPUP_WIDTH;
    }
}

function getStoredPopupHeight() {
    try {
        const stored = localStorage.getItem(LABELING_POPUP_HEIGHT_STORAGE_KEY);
        if (stored == null) return LABELING_POPUP_HEIGHT;
        const n = Number(stored);
        if (!Number.isFinite(n)) return LABELING_POPUP_HEIGHT;
        return Math.max(LABELING_POPUP_MIN_HEIGHT, Math.min(LABELING_POPUP_MAX_HEIGHT, Math.round(n)));
    } catch {
        return LABELING_POPUP_HEIGHT;
    }
}

function setStoredPopupSize(width, height) {
    try {
        localStorage.setItem(LABELING_POPUP_WIDTH_STORAGE_KEY, String(width));
        localStorage.setItem(LABELING_POPUP_HEIGHT_STORAGE_KEY, String(height));
    } catch (_) {}
}

/** Resize handle identifiers for edges and corners. */
const RESIZE_HANDLES = ['left', 'right', 'top', 'bottom', 'topLeft', 'topRight', 'bottomLeft', 'bottomRight'];

/**
 * One popup for labeling mode: lists all selected points with color, date, time, value.
 * Positioned at top-right of the map. Draggable via the header; resizable from any edge or corner. Size persisted in localStorage.
 */
class LabelingMapPopup extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            right: LABELING_POPUP_OFFSET,
            top: LABELING_POPUP_OFFSET,
            width: getStoredPopupWidth(),
            height: getStoredPopupHeight(),
            dragging: false,
            dragStart: null,
            resizing: false,
            resizeStart: null,
        };
        this.popupRef = React.createRef();
        this.handleMouseDown = this.handleMouseDown.bind(this);
        this.handleMouseMove = this.handleMouseMove.bind(this);
        this.handleMouseUp = this.handleMouseUp.bind(this);
        this.handleResizeMouseDown = this.handleResizeMouseDown.bind(this);
        this.handleResizeMouseMove = this.handleResizeMouseMove.bind(this);
        this.handleResizeMouseUp = this.handleResizeMouseUp.bind(this);
    }

    componentDidMount() {
        document.addEventListener('mousemove', this.handleMouseMove);
        document.addEventListener('mouseup', this.handleMouseUp);
        document.addEventListener('mousemove', this.handleResizeMouseMove);
        document.addEventListener('mouseup', this.handleResizeMouseUp);
    }

    componentWillUnmount() {
        document.removeEventListener('mousemove', this.handleMouseMove);
        document.removeEventListener('mouseup', this.handleMouseUp);
        document.removeEventListener('mousemove', this.handleResizeMouseMove);
        document.removeEventListener('mouseup', this.handleResizeMouseUp);
    }

    handleMouseDown(e) {
        if (e.button !== 0) return;
        e.preventDefault();
        this.setState({
            dragging: true,
            dragStart: {
                startX: e.clientX,
                startY: e.clientY,
                startRight: this.state.right,
                startTop: this.state.top,
            },
        });
    }

    handleMouseMove(e) {
        if (!this.state.dragging || !this.state.dragStart) return;
        const { startX, startY, startRight, startTop } = this.state.dragStart;
        let newRight = startRight + (startX - e.clientX);
        let newTop = startTop + (e.clientY - startY);
        const wrapper = this.popupRef.current?.parentElement;
        if (wrapper) {
            const wr = wrapper.getBoundingClientRect();
            const popupRect = this.popupRef.current.getBoundingClientRect();
            const maxRight = wr.width - popupRect.width;
            const maxTop = wr.height - popupRect.height;
            newRight = Math.max(0, Math.min(maxRight, newRight));
            newTop = Math.max(0, Math.min(maxTop, newTop));
        }
        this.setState({ right: newRight, top: newTop });
    }

    handleMouseUp() {
        this.setState({ dragging: false, dragStart: null });
    }

    handleResizeMouseDown(e, handle) {
        if (e.button !== 0) return;
        e.preventDefault();
        e.stopPropagation();
        this.setState({
            resizing: true,
            resizeStart: {
                handle,
                startX: e.clientX,
                startY: e.clientY,
                startRight: this.state.right,
                startTop: this.state.top,
                startWidth: this.state.width,
                startHeight: this.state.height,
            },
        });
    }

    handleResizeMouseMove(e) {
        if (!this.state.resizing || !this.state.resizeStart) return;
        const { handle, startX, startY, startRight, startTop, startWidth, startHeight } = this.state.resizeStart;
        const dx = e.clientX - startX;
        const dy = e.clientY - startY;

        let newRight = startRight;
        let newTop = startTop;
        let newWidth = startWidth;
        let newHeight = startHeight;

        const left = handle.includes('left');
        const right = handle.includes('right');
        const top = handle.includes('top');
        const bottom = handle.includes('bottom');

        // Popup is positioned with right/top. Right edge = container right - right. Left edge stays fixed when resizing right handle; right edge stays fixed when resizing left handle.
        if (left) {
            newWidth = startWidth + (startX - e.clientX);
            newWidth = Math.max(LABELING_POPUP_MIN_WIDTH, Math.min(LABELING_POPUP_MAX_WIDTH, newWidth));
            newRight = startRight;
        } else if (right) {
            newWidth = startWidth + dx;
            newWidth = Math.max(LABELING_POPUP_MIN_WIDTH, Math.min(LABELING_POPUP_MAX_WIDTH, newWidth));
            newRight = startRight - (newWidth - startWidth);
        }

        if (top) {
            newHeight = startHeight + (startY - e.clientY);
            newHeight = Math.max(LABELING_POPUP_MIN_HEIGHT, Math.min(LABELING_POPUP_MAX_HEIGHT, newHeight));
            newTop = startTop + (startHeight - newHeight);
        } else if (bottom) {
            newHeight = startHeight + dy;
            newHeight = Math.max(LABELING_POPUP_MIN_HEIGHT, Math.min(LABELING_POPUP_MAX_HEIGHT, newHeight));
        }

        this.setState({ right: newRight, top: newTop, width: newWidth, height: newHeight });
    }

    handleResizeMouseUp() {
        if (this.state.resizing && this.state.width != null && this.state.height != null) {
            setStoredPopupSize(this.state.width, this.state.height);
        }
        this.setState({ resizing: false, resizeStart: null });
    }

    renderResizeHandle(handle) {
        const size = RESIZE_HANDLE_SIZE;
        const cursors = {
            left: 'w-resize',
            right: 'e-resize',
            top: 'n-resize',
            bottom: 's-resize',
            topLeft: 'nw-resize',
            topRight: 'ne-resize',
            bottomLeft: 'sw-resize',
            bottomRight: 'se-resize',
        };
        const base = {
            position: 'absolute',
            zIndex: 2,
            background: 'transparent',
        };
        let style = { ...base, cursor: cursors[handle] };
        if (handle === 'left') {
            style = { ...style, left: 0, top: 0, bottom: 0, width: size };
        } else if (handle === 'right') {
            style = { ...style, right: 0, top: 0, bottom: 0, width: size };
        } else if (handle === 'top') {
            style = { ...style, left: 0, top: 0, right: 0, height: size };
        } else if (handle === 'bottom') {
            style = { ...style, left: 0, right: 0, bottom: 0, height: size };
        } else if (handle === 'topLeft') {
            style = { ...style, left: 0, top: 0, width: size, height: size };
        } else if (handle === 'topRight') {
            style = { ...style, right: 0, top: 0, width: size, height: size };
        } else if (handle === 'bottomLeft') {
            style = { ...style, left: 0, bottom: 0, width: size, height: size };
        } else {
            style = { ...style, right: 0, bottom: 0, width: size, height: size };
        }
        return (
            <div
                key={handle}
                onMouseDown={(e) => this.handleResizeMouseDown(e, handle)}
                title="Drag to resize"
                style={style}
            />
        );
    }

    render() {
        const { paramName, sections, startDateTime, pendingSectionStart, onUpdateLabel, onRemoveSection, onClearAll, onClose } = this.props;
        const list = sections || [];
        const { right, top, width, height, dragging, resizing } = this.state;

        const wrapperStyle = {
            position: 'absolute',
            right,
            top,
            width,
            height,
            minWidth: LABELING_POPUP_MIN_WIDTH,
            minHeight: LABELING_POPUP_MIN_HEIGHT,
            maxHeight: '90vh',
            zIndex: 10000,
            pointerEvents: 'auto',
            background: '#fff',
            border: '1px solid rgba(0,0,0,0.2)',
            borderRadius: '0.375rem',
            boxShadow: '0 0.5rem 1rem rgba(0,0,0,0.15)',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            userSelect: 'none',
        };

        const fmtVal = (v) => v != null ? (typeof v === 'number' ? v.toFixed(3) : String(v)) : '—';

        return (
            <div ref={this.popupRef} style={wrapperStyle} className="labeling-map-popup">
                {RESIZE_HANDLES.map((h) => this.renderResizeHandle(h))}
                <div
                    onMouseDown={this.handleMouseDown}
                    style={{
                        padding: '8px 12px',
                        background: 'var(--c_row_bg_alt, #f0f0f0)',
                        borderBottom: '1px solid var(--c_border_alt, #dee2e6)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        cursor: dragging ? 'grabbing' : 'grab',
                    }}
                    title="Drag to move"
                >
                    <strong style={{ fontSize: '0.9em' }}>{paramName || 'Sections'} ({list.length})</strong>
                    <span onClick={(e) => e.stopPropagation()}>
                        {onClearAll && (
                            <Button variant="outline-secondary" size="sm" className="me-1" onClick={onClearAll}>
                                Clear all
                            </Button>
                        )}
                        {onClose && (
                            <Button variant="outline-secondary" size="sm" onClick={onClose}>
                                <i className="fa fa-times"/> Close
                            </Button>
                        )}
                    </span>
                </div>
                <div style={{ padding: 8, overflow: 'auto', flex: 1, fontSize: '0.75em' }}>
                    {pendingSectionStart && (
                        <div className="text-muted mb-2" style={{ fontSize: '0.9em' }}>
                            Click on path to set section end.
                        </div>
                    )}
                    <table className="table table-sm table-bordered mb-0">
                        <thead>
                            <tr>
                                <th style={{ width: 24 }}></th>
                                <th style={{ fontSize: '0.7em' }}>Start</th>
                                <th style={{ fontSize: '0.7em' }}>End</th>
                                <th style={{ fontSize: '0.7em' }}>Val start</th>
                                <th style={{ fontSize: '0.7em' }}>Val end</th>
                                <th style={{ fontSize: '0.7em' }}>Label</th>
                                <th style={{ width: 32 }}></th>
                            </tr>
                        </thead>
                        <tbody>
                            {list.map((sec, i) => (
                                <tr key={i}>
                                    <td>
                                        <span style={{
                                            display: 'inline-block',
                                            width: 14,
                                            height: 14,
                                            borderRadius: '50%',
                                            background: LABELING_MARKER_COLORS[i % LABELING_MARKER_COLORS.length],
                                            border: '1px solid #fff',
                                            boxShadow: '0 0 0 1px rgba(0,0,0,0.2)',
                                        }} title={`Section ${i + 1}`}/>
                                    </td>
                                    <td style={{ fontSize: '0.8em' }}>
                                        {formatLabelingDate(sec.startTime, startDateTime)} {formatLabelingTimeOnly(sec.startTime, startDateTime)}
                                    </td>
                                    <td style={{ fontSize: '0.8em' }}>
                                        {formatLabelingDate(sec.endTime, startDateTime)} {formatLabelingTimeOnly(sec.endTime, startDateTime)}
                                    </td>
                                    <td style={{ fontSize: '0.8em' }}>{fmtVal(sec.startValue)}</td>
                                    <td style={{ fontSize: '0.8em' }}>{fmtVal(sec.endValue)}</td>
                                    <td style={{ padding: '2px 4px' }}>
                                        <input
                                            type="text"
                                            className="form-control form-control-sm"
                                            style={{ fontSize: '0.9em', minWidth: 80 }}
                                            placeholder="Comment..."
                                            value={sec.label ?? ''}
                                            onChange={(e) => onUpdateLabel && onUpdateLabel(i, e.target.value)}
                                            onClick={(e) => e.stopPropagation()}
                                            title="Click to edit label"
                                        />
                                    </td>
                                    <td>
                                        {onRemoveSection && (
                                            <Button variant="outline-danger" size="sm" className="p-0" style={{ minWidth: 28 }} onClick={() => onRemoveSection(i)} title="Remove section">
                                                <i className="fa fa-times"/>
                                            </Button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                <div
                    style={{
                        position: 'absolute',
                        left: 4,
                        top: 4,
                        fontSize: 10,
                        color: 'rgba(0,0,0,0.35)',
                        pointerEvents: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}
                    title="Resizable from any edge or corner"
                >
                    <i className="fa fa-arrows-alt" aria-hidden="true"/>
                </div>
            </div>
        );
    }
}

/**
 * Brief hover tooltip: just date/time and value (one line).
 */
function LabelingHoverTooltip({ placement, time, value, startDateTime, navbarWidth = 40 }) {
    if (!placement || placement.length < 2) return null;
    const timeStr = formatLabelingDateTime(time, startDateTime);
    const valueStr = value != null ? (typeof value === 'number' ? value.toFixed(3) : String(value)) : '—';
    return (
        <div
            style={{
                position: 'fixed',
                top: placement[1] + navbarWidth,
                left: placement[0],
                zIndex: 999,
                padding: '2px 6px',
                fontSize: '12px',
                background: 'rgba(0,0,0,0.8)',
                color: '#fff',
                borderRadius: '4px',
                whiteSpace: 'nowrap',
                pointerEvents: 'none',
            }}
        >
            {timeStr} &nbsp; {valueStr}
        </div>
    );
}

/**
 * Draggable side panel inside the map container. Initially at top-right of map.
 */
class LabelingSidePanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            left: null,
            top: null,
            right: 12,
            topOffset: 12,
            dragging: false,
            dragStart: null,
        };
        this.panelRef = React.createRef();
        this.handleMouseDown = this.handleMouseDown.bind(this);
        this.handleMouseMove = this.handleMouseMove.bind(this);
        this.handleMouseUp = this.handleMouseUp.bind(this);
    }

    componentDidMount() {
        document.addEventListener('mousemove', this.handleMouseMove);
        document.addEventListener('mouseup', this.handleMouseUp);
    }

    componentWillUnmount() {
        document.removeEventListener('mousemove', this.handleMouseMove);
        document.removeEventListener('mouseup', this.handleMouseUp);
    }

    handleMouseDown(e) {
        if (e.button !== 0) return;
        const { container } = this.props;
        const panel = this.panelRef.current;
        if (!container || !panel) return;
        const cRect = container.getBoundingClientRect();
        const pRect = panel.getBoundingClientRect();
        const leftRel = pRect.left - cRect.left;
        const topRel = pRect.top - cRect.top;
        this.setState({
            dragging: true,
            dragStart: { startX: e.clientX, startY: e.clientY, leftRel, topRel },
            left: leftRel,
            top: topRel,
            right: null,
            topOffset: null,
        });
    }

    handleMouseMove(e) {
        if (!this.state.dragging || !this.state.dragStart) return;
        const { container } = this.props;
        if (!container) return;
        const cRect = container.getBoundingClientRect();
        const { startX, startY, leftRel, topRel } = this.state.dragStart;
        const panelWidth = 280;
        const panelHeight = 320;
        let newLeft = leftRel + (e.clientX - startX);
        let newTop = topRel + (e.clientY - startY);
        newLeft = Math.max(0, Math.min(cRect.width - panelWidth, newLeft));
        newTop = Math.max(0, Math.min(cRect.height - panelHeight, newTop));
        this.setState({ left: newLeft, top: newTop });
    }

    handleMouseUp() {
        this.setState({ dragging: false, dragStart: null });
    }

    render() {
        const { paramName, points, startDateTime, onRemovePoint, onClearAll } = this.props;
        if (!points || points.length === 0) return null;
        const { left, top, right, topOffset, dragging } = this.state;
        const position = left != null && top != null
            ? { position: 'absolute', left, top }
            : { position: 'absolute', right: right ?? 12, top: topOffset ?? 12 };
        return (
            <div
                ref={this.panelRef}
                style={{
                    ...position,
                    width: 280,
                    maxHeight: '70vh',
                    display: 'flex',
                    flexDirection: 'column',
                    zIndex: 1000,
                    background: 'var(--c_entry_bg, #fff)',
                    border: '1px solid var(--c_border_alt, #dee2e6)',
                    borderRadius: 8,
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    overflow: 'hidden',
                    userSelect: 'none',
                }}
            >
                <div
                    onMouseDown={this.handleMouseDown}
                    style={{
                        padding: '8px 12px',
                        cursor: dragging ? 'grabbing' : 'grab',
                        background: 'var(--c_row_bg_alt, #f0f0f0)',
                        borderBottom: '1px solid var(--c_border_alt, #dee2e6)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                    }}
                >
                    <strong style={{ fontSize: '0.9em' }}>{paramName || 'Points'}</strong>
                    <Button variant="outline-secondary" size="sm" onClick={onClearAll}>
                        Clear all
                    </Button>
                </div>
                <div style={{ padding: 12, overflow: 'auto', flex: 1 }}>
                    <table className="table table-sm table-bordered mb-0">
                        <thead>
                            <tr>
                                <th style={{ width: 24 }}></th>
                                <th style={{ fontSize: '0.7em' }}>Date</th>
                                <th style={{ fontSize: '0.7em' }}>Time</th>
                                <th style={{ fontSize: '0.7em' }}>Value</th>
                                <th style={{ width: 32 }}></th>
                            </tr>
                        </thead>
                        <tbody>
                            {points.map((pt, i) => (
                                <tr key={i}>
                                    <td>
                                        <span style={{
                                            display: 'inline-block',
                                            width: 14,
                                            height: 14,
                                            borderRadius: '50%',
                                            background: LABELING_MARKER_COLORS[i % LABELING_MARKER_COLORS.length],
                                            border: '1px solid #fff',
                                            boxShadow: '0 0 0 1px rgba(0,0,0,0.2)',
                                        }} title={`Point ${i + 1}`}/>
                                    </td>
                                    <td style={{ fontSize: '0.8em' }}>{formatLabelingDate(pt.time, startDateTime)}</td>
                                    <td style={{ fontSize: '0.8em' }}>{formatLabelingTimeOnly(pt.time, startDateTime)}</td>
                                    <td style={{ fontSize: '0.8em' }}>{pt.value != null ? (typeof pt.value === 'number' ? pt.value.toFixed(3) : String(pt.value)) : '—'}</td>
                                    <td>
                                        <Button variant="outline-danger" size="sm" className="p-0" style={{ minWidth: 28 }} onClick={() => onRemovePoint(i)} title="Remove">
                                            <i className="fa fa-times"/>
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }
}

export { LabelingMapPopup, LabelingHoverTooltip, LabelingSidePanel };
