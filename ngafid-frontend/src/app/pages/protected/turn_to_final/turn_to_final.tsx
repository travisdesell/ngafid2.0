import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import PanelAlert from "@/components/panel_alert";
import { getLogger } from "@/components/providers/logger";
import TimeHeader from "@/components/providers/time_header/time_header";
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Slider } from "@/components/ui/slider";
import { fetchJson } from "@/fetchJson";
import { ChartNoAxesCombined, Filter, GaugeCircle, Loader2, MapPinned, Plane } from "lucide-react";
import Feature from "ol/Feature.js";
import Map from "ol/Map.js";
import Overlay from "ol/Overlay.js";
import View from "ol/View.js";
import { isEmpty as extentIsEmpty } from "ol/extent.js";
import LineString from "ol/geom/LineString.js";
import Point from "ol/geom/Point.js";
import TileLayer from "ol/layer/Tile.js";
import VectorLayer from "ol/layer/Vector.js";
import "ol/ol.css";
import { fromLonLat } from "ol/proj.js";
import OSM from "ol/source/OSM.js";
import VectorSource from "ol/source/Vector.js";
import XYZ from "ol/source/XYZ.js";
import { Circle as CircleStyle, Stroke, Style } from "ol/style.js";
import { useEffect, useMemo, useRef, useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis } from "recharts";

const log = getLogger("TurnToFinal", "black", "Page");

const ROLL_THRESHOLDS = {
	Min: 0,
	Default: 0,
	Dangerous: 26,
	MaxSoft: 30,
	MaxHard: 45,
} as const;

const RUNWAY_ANY = "Any Runway";

type AirportInfo = {
	iataCode?: string;
	latitude?: number;
	longitude?: number;
};

type RunwayInfo = {
	name?: string;
};

type TurnToFinalApi = {
	flightId?: string;
	flight_id?: string;
	airportIataCode?: string;
	runway?: RunwayInfo;
	flightStartDate?: string;
	maxRoll?: number;
	selfDefinedGlideAngle?: number;
	selfDefinedGlidePathDeviations?: number[];
	distanceFromRunway?: number[];
	AltAGL?: number[];
	latitude?: number[];
	longitude?: number[];
	lat?: number[];
	lon?: number[];
	approachn?: number;
};

type TurnToFinalNormalized = {
	id: string;
	flightId: string;
	approachn: number;
	airportIataCode: string;
	runwayName: string;
	flightStartDate: Date | null;
	maxRoll: number;
	selfDefinedGlideAngle: number;
	deviations: number[];
	distanceFromRunway: number[];
	altAgl: number[];
	pointsLonLat: Array<[number, number]>;
};

type TurnToFinalResponse = {
	airports?: Record<string, AirportInfo>;
	ttfs?: TurnToFinalApi[];
};

type CachedPayload = {
	airport: string;
	startDate: string;
	endDate: string;
	airports: Record<string, AirportInfo>;
	ttfs: TurnToFinalNormalized[];
};

type PopupData = {
	flightId: string;
	approachn: number;
};

type LegacyWindow = Window & {
	airports?: unknown;
	runways?: unknown;
};

const toDate = (input: string | undefined | null): Date | null => {
	if (!input)
		return null;

	const parsed = new Date(input);
	if (!Number.isNaN(parsed.getTime()))
		return parsed;

	return null;
};

const toFiniteNumberArray = (arr: unknown): number[] => {
	if (!Array.isArray(arr))
		return [];

	return arr
		.map((value) => Number(value))
		.filter((value) => Number.isFinite(value));
};

const normalizeTurnToFinalRows = (rowsRaw: TurnToFinalApi[]): TurnToFinalNormalized[] => {
	const rows = rowsRaw ?? [];
	const approachCounts: Record<string, number> = {};

	return rows.map((row, rowIndex) => {
		const flightId = String(row.flightId ?? row.flight_id ?? `Approach-${rowIndex + 1}`);
		approachCounts[flightId] = (approachCounts[flightId] ?? 0) + 1;
		const approachn = Number.isFinite(row.approachn) ? Number(row.approachn) : approachCounts[flightId]!;

		const lat = toFiniteNumberArray(row.lat ?? row.latitude);
		const lon = toFiniteNumberArray(row.lon ?? row.longitude);
		const pointsLonLat: Array<[number, number]> = [];

		const pointsCount = Math.min(lat.length, lon.length);
		for (let pointIndex = 0; pointIndex < pointsCount; pointIndex++) {
			pointsLonLat.push([lon[pointIndex]!, lat[pointIndex]!]);
		}

		const distanceFromRunway = toFiniteNumberArray(row.distanceFromRunway);
		const deviations = toFiniteNumberArray(row.selfDefinedGlidePathDeviations);
		const altAgl = toFiniteNumberArray(row.AltAGL);

		return {
			id: `${flightId}::${approachn}::${rowIndex}`,
			flightId,
			approachn,
			airportIataCode: String(row.airportIataCode ?? ""),
			runwayName: String(row.runway?.name ?? "Unknown"),
			flightStartDate: toDate(row.flightStartDate),
			maxRoll: Number.isFinite(Number(row.maxRoll)) ? Number(row.maxRoll) : 0,
			selfDefinedGlideAngle: Number.isFinite(Number(row.selfDefinedGlideAngle)) ? Number(row.selfDefinedGlideAngle) : 0,
			deviations,
			distanceFromRunway,
			altAgl,
			pointsLonLat,
		};
	});
};

const dateWithinRange = (value: Date | null, startDate: Date, endDate: Date): boolean => {
	if (!value)
		return true;
	return startDate <= value && value <= endDate;
};

const toHex = (value: number): string => Math.max(0, Math.min(255, Math.round(value))).toString(16).padStart(2, "0");

const colorForRoll = (roll: number): string => {
	const tDanger = (Math.max(ROLL_THRESHOLDS.Min, Math.min(ROLL_THRESHOLDS.Dangerous, roll)) - ROLL_THRESHOLDS.Min)
		/ Math.max(1, ROLL_THRESHOLDS.Dangerous - ROLL_THRESHOLDS.Min);
	const tSoft = (Math.max(ROLL_THRESHOLDS.Dangerous, Math.min(ROLL_THRESHOLDS.MaxSoft, roll)) - ROLL_THRESHOLDS.Dangerous)
		/ Math.max(1, ROLL_THRESHOLDS.MaxSoft - ROLL_THRESHOLDS.Dangerous);

	let r = 0;
	let g = 255;
	if (roll <= ROLL_THRESHOLDS.Dangerous) {
		r = 255 * tDanger;
		g = 255;
	} else {
		r = 255;
		g = 255 * (1 - tSoft);
	}

	return `#${toHex(r)}${toHex(g)}00`;
};

const styleForRoll = (roll: number): Style => new Style({
	stroke: new Stroke({
		color: colorForRoll(roll),
		width: 2.5,
	}),
	image: new CircleStyle({
		radius: 4,
		stroke: new Stroke({
			color: "rgba(0,0,0,0)",
			width: 1,
		}),
	}),
});

export default function TurnToFinalPage() {
	useEffect(() => {
		document.title = "NGAFID — Turn to Final";
	}, []);

	const { setModal } = useModal();
	const { endpointStartDate, endpointEndDate, reapplyTrigger, appliedStartDate, appliedEndDate, renderDateRangeMonthly } = useTimeHeader();

	const [disableFetching, setDisableFetching] = useState(false);
	const [minRoll, setMinRoll] = useState<number>(ROLL_THRESHOLDS.Default);
	const [selectedAirport, setSelectedAirport] = useState("");
	const [selectedRunway, setSelectedRunway] = useState(RUNWAY_ANY);
	const [mapStyle, setMapStyle] = useState<"Road" | "Aerial">("Road");
	const [payload, setPayload] = useState<CachedPayload | null>(null);
	const [popupData, setPopupData] = useState<PopupData | null>(null);

	const mapContainerRef = useRef<HTMLDivElement | null>(null);
	const popupRef = useRef<HTMLDivElement | null>(null);
	const popupCloseRef = useRef<HTMLAnchorElement | null>(null);
	const mapRef = useRef<Map | null>(null);
	const overlayRef = useRef<Overlay | null>(null);
	const vectorLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
	const vectorSourceRef = useRef<VectorSource | null>(null);
	const roadLayerRef = useRef<TileLayer<OSM> | null>(null);
	const aerialLayerRef = useRef<TileLayer<XYZ> | null>(null);
	const cacheRef = useRef<CachedPayload | null>(null);

	const legacyGlobals = (window as LegacyWindow);

	const fallbackAirportList = useMemo<string[]>(() => {
		const value = legacyGlobals.airports;

		if (Array.isArray(value))
			return value.map((airportCode) => String(airportCode)).filter((airportCode) => airportCode.length > 0);

		if (value && typeof value === "object")
			return Object.keys(value as Record<string, unknown>);

		return [];
	}, [legacyGlobals.airports]);

	const fallbackRunwaysByAirport = useMemo<Record<string, string[]>>(() => {
		const value = legacyGlobals.runways;
		if (!value || typeof value !== "object")
			return {};

		const out: Record<string, string[]> = {};
		for (const [airportCode, runwaysRaw] of Object.entries(value as Record<string, unknown>)) {
			if (!Array.isArray(runwaysRaw))
				continue;

			out[airportCode] = runwaysRaw
				.map((runwayRaw) => {
					if (runwayRaw && typeof runwayRaw === "object" && "name" in runwayRaw)
						return String((runwayRaw as RunwayInfo).name ?? "");
					return String(runwayRaw ?? "");
				})
				.filter((runwayName) => runwayName.length > 0)
				.sort((a, b) => a.localeCompare(b));
		}

		return out;
	}, [legacyGlobals.runways]);

	useEffect(() => {
		if (selectedAirport)
			return;
		if (fallbackAirportList.length === 0)
			return;

		setSelectedAirport(fallbackAirportList[0]!);
	}, [fallbackAirportList, selectedAirport]);

	const fetchTurnToFinalData = async () => {
		if (!selectedAirport) {
			setModal(ErrorModal, {
				title: "Turn to Final Airport Not Selected",
				message: "Select an airport first. If no airports are listed, the fleet may not have any visited-airport records yet.",
			});
			return;
		}

		const requestStartDate = endpointStartDate;
		const requestEndDate = endpointEndDate;

		const requestStartDateObject = toDate(requestStartDate);
		const requestEndDateObject = toDate(requestEndDate);
		const cached = cacheRef.current;

		if (
			cached &&
			cached.airport === selectedAirport &&
			requestStartDateObject &&
			requestEndDateObject &&
			toDate(cached.startDate) &&
			toDate(cached.endDate) &&
			dateWithinRange(requestStartDateObject, toDate(cached.startDate)!, toDate(cached.endDate)!) &&
			dateWithinRange(requestEndDateObject, toDate(cached.startDate)!, toDate(cached.endDate)!)
		) {
			log("Using cached Turn-to-Final payload");
			setPayload(cached);
			return;
		}

		setDisableFetching(true);

		const params = new URLSearchParams({
			startDate: requestStartDate,
			endDate: requestEndDate,
			airport: selectedAirport,
		});

		const response = await fetchJson.get<TurnToFinalResponse>("/api/flight/turn-to-final", { params }).catch((error) => {
			setModal(ErrorModal, {
				title: "Error fetching Turn to Final data",
				message: String(error),
			});
			return null;
		});

		if (!response) {
			setDisableFetching(false);
			return;
		}

		const normalizedRows = normalizeTurnToFinalRows(response.ttfs ?? []);
		const nextPayload: CachedPayload = {
			airport: selectedAirport,
			startDate: requestStartDate,
			endDate: requestEndDate,
			airports: response.airports ?? {},
			ttfs: normalizedRows,
		};

		cacheRef.current = nextPayload;
		setPayload(nextPayload);
		setDisableFetching(false);
	};

	useEffect(() => {
		if (!selectedAirport)
			return;

		fetchTurnToFinalData();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [reapplyTrigger]);

	const airportOptions = useMemo(() => {
		const set = new Set<string>();

		for (const airportCode of fallbackAirportList)
			set.add(airportCode);

		for (const airportCode of Object.keys(payload?.airports ?? {}))
			set.add(airportCode);

		for (const row of payload?.ttfs ?? []) {
			if (row.airportIataCode)
				set.add(row.airportIataCode);
		}

		return Array.from(set).sort((a, b) => a.localeCompare(b));
	}, [fallbackAirportList, payload]);

	const runwayOptions = useMemo(() => {
		const set = new Set<string>();

		for (const runway of (fallbackRunwaysByAirport[selectedAirport] ?? []))
			set.add(runway);

		for (const row of payload?.ttfs ?? []) {
			if (row.airportIataCode === selectedAirport && row.runwayName)
				set.add(row.runwayName);
		}

		return [RUNWAY_ANY, ...Array.from(set).sort((a, b) => a.localeCompare(b))];
	}, [fallbackRunwaysByAirport, payload, selectedAirport]);

	const filteredRows = useMemo(() => {
		const rows = payload?.ttfs ?? [];

		return rows.filter((row) => {
			if (row.airportIataCode !== selectedAirport)
				return false;

			if (!dateWithinRange(row.flightStartDate, appliedStartDate, appliedEndDate))
				return false;

			if (selectedRunway !== RUNWAY_ANY && row.runwayName !== selectedRunway)
				return false;

			if (row.maxRoll < minRoll)
				return false;

			return true;
		});
	}, [appliedEndDate, appliedStartDate, minRoll, payload, selectedAirport, selectedRunway]);

	useEffect(() => {
		if (!mapContainerRef.current)
			return;

		const roadLayer = new TileLayer({
			source: new OSM(),
			visible: true,
		});
		const aerialLayer = new TileLayer({
			source: new XYZ({
				url: "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
			}),
			visible: false,
		});

		roadLayerRef.current = roadLayer;
		aerialLayerRef.current = aerialLayer;

		const vectorSource = new VectorSource();
		const vectorLayer = new VectorLayer({ source: vectorSource });
		vectorSourceRef.current = vectorSource;
		vectorLayerRef.current = vectorLayer;

		const overlay = popupRef.current
			? new Overlay({
				element: popupRef.current,
				autoPan: true,
			  })
			: null;

		overlayRef.current = overlay;

		const map = new Map({
			target: mapContainerRef.current,
			layers: [roadLayer, aerialLayer, vectorLayer],
			overlays: overlay ? [overlay] : [],
			view: new View({
				center: fromLonLat([-97.0329, 47.9253]),
				zoom: 4,
				minZoom: 0,
				maxZoom: 18,
			}),
            controls: [],
		});

		mapRef.current = map;

		if (popupCloseRef.current && overlay) {
			popupCloseRef.current.onclick = (event) => {
				event.preventDefault();
				overlay.setPosition(undefined);
				setPopupData(null);
			};
		}

		map.on("singleclick", (event) => {
			if (!overlay)
				return;

			const feature = map.forEachFeatureAtPixel(event.pixel, (featureAtPixel) => featureAtPixel as Feature);
			const row = feature?.get("ttf") as TurnToFinalNormalized | undefined;
			if (!row) {
				overlay.setPosition(undefined);
				setPopupData(null);
				return;
			}

			setPopupData({
				flightId: row.flightId,
				approachn: row.approachn,
			});
			overlay.setPosition(event.coordinate);
		});

		return () => {
			try {
				overlayRef.current?.setPosition(undefined);
				map.setTarget(undefined);
			} catch {
				/* ... */
			}

			mapRef.current = null;
			overlayRef.current = null;
			vectorLayerRef.current = null;
			vectorSourceRef.current = null;
			roadLayerRef.current = null;
			aerialLayerRef.current = null;
		};
	}, []);

	useEffect(() => {
		if (!roadLayerRef.current || !aerialLayerRef.current)
			return;

		roadLayerRef.current.setVisible(mapStyle === "Road");
		aerialLayerRef.current.setVisible(mapStyle === "Aerial");
	}, [mapStyle]);

	useEffect(() => {
		const vectorSource = vectorSourceRef.current;
		const map = mapRef.current;
		if (!vectorSource || !map)
			return;

		vectorSource.clear();

		const features: Feature[] = [];

		for (const row of filteredRows) {
			if (row.pointsLonLat.length === 0)
				continue;

			const points = row.pointsLonLat.map((point) => fromLonLat(point));
			const pathFeature = new Feature({
				geometry: new LineString(points),
				ttf: row,
			});
			pathFeature.setStyle(styleForRoll(row.maxRoll));
			features.push(pathFeature);

			const markerFeature = new Feature({
				geometry: new Point(points[0]!),
				ttf: row,
			});
			markerFeature.setStyle(styleForRoll(row.maxRoll));
			features.push(markerFeature);
		}

		vectorSource.addFeatures(features);

		const view = map.getView();
		const extent = vectorSource.getExtent();

		if (extent && !extentIsEmpty(extent)) {
			view.fit(extent, {
				padding: [40, 40, 40, 40],
				maxZoom: 15,
			});
			return;
		}

		const airportCenter = payload?.airports?.[selectedAirport];
		if (airportCenter && Number.isFinite(airportCenter.longitude) && Number.isFinite(airportCenter.latitude)) {
			view.setCenter(fromLonLat([airportCenter.longitude!, airportCenter.latitude!]));
			view.setZoom(11);
		}
	}, [filteredRows, payload, selectedAirport]);

	const deviationsSeries = useMemo(() => {
		return filteredRows.map((row) => {
			const pointCount = Math.min(row.distanceFromRunway.length, row.deviations.length);
			const data: Array<{ x: number; y: number }> = [];

			for (let index = 0; index < pointCount; index++) {
				data.push({
					x: row.distanceFromRunway[index]!,
					y: row.deviations[index]!,
				});
			}

			return {
				id: row.id,
				name: `${row.flightId} (App ${row.approachn})`,
				color: colorForRoll(row.maxRoll),
				data,
			};
		}).filter((series) => series.data.length > 0);
	}, [filteredRows]);

	const altitudeSeries = useMemo(() => {
		return filteredRows.map((row) => {
			const pointCount = Math.min(row.distanceFromRunway.length, row.altAgl.length);
			const data: Array<{ x: number; y: number }> = [];

			for (let index = 0; index < pointCount; index++) {
				data.push({
					x: row.distanceFromRunway[index]!,
					y: row.altAgl[index]!,
				});
			}

			return {
				id: row.id,
				name: `${row.flightId} (App ${row.approachn})`,
				color: colorForRoll(row.maxRoll),
				data,
			};
		}).filter((series) => series.data.length > 0);
	}, [filteredRows]);

	const histogramData = useMemo(() => {
		const binMin = ROLL_THRESHOLDS.Min;
		const binMax = ROLL_THRESHOLDS.MaxHard;
		const bins = Array.from({ length: binMax - binMin + 1 }, (_, index) => ({
			angle: binMin + index,
			count: 0,
		}));

		for (const row of filteredRows) {
			const value = Math.round(row.selfDefinedGlideAngle);
			if (value < binMin || value > binMax)
				continue;
			bins[value - binMin]!.count += 1;
		}

		return bins;
	}, [filteredRows]);

	const ttfCount = filteredRows.length;

	const renderNoDataMessage = () => (
		<PanelAlert
			title="No Turn-to-Final Data"
			description={[
				"No matching approaches were found for the selected filters.",
				"Try reducing the roll threshold or changing runway/date filters.",
			]}
		/>
	);

	return (
		<div className="page-container">
			<div className="page-content gap-4">

				<TimeHeader
					onApply={() => {
						log("Applying Turn-to-Final date range");
					}}
					dependencies={[selectedAirport]}
				>
					<div className="flex flex-col gap-2 min-w-48">
						<Select
							value={selectedAirport}
							onValueChange={(airportCode) => {
								setSelectedAirport(airportCode);
								setSelectedRunway(RUNWAY_ANY);
							}}
							disabled={disableFetching || airportOptions.length === 0}
						>
							<SelectTrigger>
								<SelectValue placeholder="Select airport" />
							</SelectTrigger>
							<SelectContent>
								{airportOptions.map((airportCode) => (
									<SelectItem key={airportCode} value={airportCode}>{airportCode}</SelectItem>
								))}
							</SelectContent>
						</Select>
					</div>

					<div className="flex flex-col gap-2 min-w-48">
						<Select
							value={selectedRunway}
							onValueChange={(runwayName) => setSelectedRunway(runwayName)}
							disabled={disableFetching || runwayOptions.length === 0}
						>
							<SelectTrigger>
								<SelectValue placeholder="Select runway" />
							</SelectTrigger>
							<SelectContent>
								{runwayOptions.map((runwayName) => (
									<SelectItem key={runwayName} value={runwayName}>{runwayName}</SelectItem>
								))}
							</SelectContent>
						</Select>
					</div>

					<div className="flex flex-col gap-4 min-w-48">
						<Label className="px-0 flex w-full justify-between">
                            <span>Minimum Roll Threshold:</span>
                            <span className="w-8">{minRoll.toFixed(0)}°</span>
                        </Label>
						<Slider
							min={ROLL_THRESHOLDS.Min}
							max={ROLL_THRESHOLDS.MaxHard}
							step={1}
							value={[minRoll]}
							onValueChange={(value) => setMinRoll(value[0] ?? ROLL_THRESHOLDS.Min)}
						/>
					</div>
				</TimeHeader>

				{/* <div className="flex flex-wrap gap-2">
					<Badge variant="outline" className="pointer-events-none px-3 py-1.5"><Plane size={14} /> Approaches: {ttfCount.toLocaleString()}</Badge>
					<Badge variant="outline" className="pointer-events-none px-3 py-1.5"><GaugeCircle size={14} /> Roll ≥ {minRoll.toFixed(0)}°</Badge>
					<Badge variant="outline" className="pointer-events-none px-3 py-1.5"><Filter size={14} /> {selectedRunway}</Badge>
					<Badge variant="outline" className="pointer-events-none px-3 py-1.5"><MapPinned size={14} /> {selectedAirport || "No Airport"}</Badge>
					{
						disableFetching
						&&
						<Badge variant="outline" className="pointer-events-none px-3 py-1.5"><Loader2 size={14} className="animate-spin" /> Fetching...</Badge>
					}
				</div> */}
{/* 
				{
					(!selectedAirport)
					&&
					<Card className="card-glossy relative min-h-40">
						<CardContent className="min-h-40 relative">
							<PanelAlert
								title="No Airport Selected"
								description={[
									"Pick an airport in the header and click Apply to load Turn-to-Final data.",
									"If the list is empty, verify the fleet has visited-airport records.",
								]}
							/>
						</CardContent>
					</Card>
				} */}

                <div className="grid grid-cols-2 grid-rows-2 gap-2">
                    <Card className="card-glossy overflow-clip relative">
                        <CardHeader className="absolute **:text-black! **:opacity-100 pointer-events-none z-10 w-full">
                            <CardTitle className="flex justify-between items-center">
                                Turn-to-Final Map
                                {renderDateRangeMonthly()}
                            </CardTitle>
                            {/* <CardDescription className="flex items-center gap-3">
                                <span>Approach paths are colored by maximum roll (green to red).</span>
                                <Select value={mapStyle} onValueChange={(value) => setMapStyle(value as "Road" | "Aerial") }>
                                    <SelectTrigger className="w-36 h-8">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="Road">Road</SelectItem>
                                        <SelectItem value="Aerial">Aerial</SelectItem>
                                    </SelectContent>
                                </Select>
                            </CardDescription> */}
                        </CardHeader>
                        <CardContent className="relative p-0">
                            <div ref={mapContainerRef} className="h-112 w-full rounded-md border p-0 m-0" />
                            <div ref={popupRef} className="ol-popup rounded-lg border bg-background/95 p-3 shadow-xl max-w-72">
                                <a ref={popupCloseRef} href="#" className="ol-popup-closer text-muted-foreground absolute right-2 top-1 text-lg" aria-label="Close popup">×</a>
                                {
                                    popupData
                                    &&
                                    <div className="flex flex-col gap-2 pr-4">
                                        <div className="text-sm font-semibold">Flight {popupData.flightId} — Approach {popupData.approachn}</div>
                                        <a className="text-sm underline text-primary" href={`/protected/flights?flight_id=${encodeURIComponent(popupData.flightId)}`} target="_blank" rel="noreferrer">Open flight in new tab</a>
                                    </div>
                                }
                            </div>
                            {
                                (selectedAirport && !disableFetching && filteredRows.length === 0)
                                &&
                                renderNoDataMessage()
                            }
                        </CardContent>
                    </Card>

                    <Card className="card-glossy">
                        <CardHeader>
                            <CardTitle className="flex justify-between">Glide Path Deviations {renderDateRangeMonthly()}</CardTitle>
                            <CardDescription>Distance above/below glide path vs distance from runway.</CardDescription>
                        </CardHeader>
                        <CardContent className="h-80">
                            {
                                deviationsSeries.length === 0
                                ? renderNoDataMessage()
                                : (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <ScatterChart margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" dataKey="x" name="Distance from runway" reversed domain={["auto", "auto"]} tickFormatter={(value) => Number(value).toFixed(0)} />
                                            <YAxis type="number" dataKey="y" name="Glide path deviation" domain={[-100, 100]} />
                                            <Tooltip cursor={{ strokeDasharray: "3 3" }} />
                                            {deviationsSeries.map((series) => (
                                                <Scatter key={series.id} data={series.data} line fill={series.color} stroke={series.color} name={series.name} />
                                            ))}
                                        </ScatterChart>
                                    </ResponsiveContainer>
                                )
                            }
                        </CardContent>
                    </Card>

                    <Card className="card-glossy">
                        <CardHeader>
                            <CardTitle className="flex justify-between">Altitude vs Distance {renderDateRangeMonthly()}</CardTitle>
                            <CardDescription>AGL altitude profile for each visible approach.</CardDescription>
                        </CardHeader>
                        <CardContent className="h-80">
                            {
                                altitudeSeries.length === 0
                                ? renderNoDataMessage()
                                : (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <ScatterChart margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" dataKey="x" name="Distance from runway" reversed domain={["auto", "auto"]} tickFormatter={(value) => Number(value).toFixed(0)} />
                                            <YAxis type="number" dataKey="y" name="Altitude AGL" domain={["auto", "auto"]} />
                                            <Tooltip cursor={{ strokeDasharray: "3 3" }} />
                                            {altitudeSeries.map((series) => (
                                                <Scatter key={series.id} data={series.data} line fill={series.color} stroke={series.color} name={series.name} />
                                            ))}
                                        </ScatterChart>
                                    </ResponsiveContainer>
                                )
                            }
                        </CardContent>
                    </Card>

                    <Card className="card-glossy">
                        <CardHeader>
                            <CardTitle className="flex justify-between">Histogram of Glide Path Angles {renderDateRangeMonthly()}</CardTitle>
                            <CardDescription>Distribution of self-defined glide path angles for filtered approaches.</CardDescription>
                        </CardHeader>
                        <CardContent className="h-80">
                            {
                                filteredRows.length === 0
                                ? renderNoDataMessage()
                                : (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={histogramData} margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis dataKey="angle" tickFormatter={(value) => `${value}°`} />
                                            <YAxis />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="var(--chart-2)" maxBarSize={20} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                )
                            }
                        </CardContent>
                    </Card>
                </div>

				{/* <Card className="card-glossy">
					<CardHeader>
						<CardTitle className="flex items-center gap-2"><ChartNoAxesCombined size={18} /> Filter Summary</CardTitle>
					</CardHeader>
					<CardContent className="text-sm text-muted-foreground flex flex-wrap gap-6">
						<div>Airport: <span className="text-foreground font-medium">{selectedAirport || "(none)"}</span></div>
						<div>Runway: <span className="text-foreground font-medium">{selectedRunway}</span></div>
						<div>Minimum roll: <span className="text-foreground font-medium">{minRoll.toFixed(0)}°</span></div>
						<div>Visible approaches: <span className="text-foreground font-medium">{ttfCount.toLocaleString()}</span></div>
					</CardContent>
				</Card> */}
			</div>
		</div>
	);
}