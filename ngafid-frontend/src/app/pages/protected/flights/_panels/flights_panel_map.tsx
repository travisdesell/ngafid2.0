// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_map.tsx

import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card } from "@/components/ui/card";
import { AlertCircle } from "lucide-react";
import { motion } from "motion/react";


// OpenLayers
import TileLayer from 'ol/layer/Tile.js';
import Map from 'ol/Map.js';
import 'ol/ol.css';
import Overlay from 'ol/Overlay.js';
import { fromLonLat } from 'ol/proj.js';
import XYZ from 'ol/source/XYZ.js';
import View from 'ol/View.js';
import { useEffect, useRef, useState } from "react";

const log = getLogger("FlightsPanelMap", "black", "Component");

type BaseStyleName =
    | 'Aerial'
    | 'Road'
    | 'RoadOnDemand'
    | 'SectionalCharts'
    | 'IFREnrouteLowCharts'
    | 'IFREnrouteHighCharts'
    | 'TerminalAreaCharts'
    | 'HelicopterCharts';


type FlightsPanelMapProps = {
    initialStyle?: BaseStyleName;
    initialCenterLonLat?: [number, number];
    initialZoom?: number;
};

function createBaseMapLayers(azureKey: string | undefined) {

    const styles: BaseStyleName[] = [
        'Aerial',
        'Road',
        'RoadOnDemand',
        'SectionalCharts',
        'IFREnrouteLowCharts',
        'IFREnrouteHighCharts',
        'TerminalAreaCharts',
        'HelicopterCharts',
    ];

    const urlFor = (name: BaseStyleName): string => {
        switch (name) {
            case 'Aerial':
                return `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}${azureKey ? `&subscription-key=${azureKey}` : ''
                    }`;
            case 'Road':
                return `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}${azureKey ? `&subscription-key=${azureKey}` : ''
                    }`;
            case 'RoadOnDemand':
                return `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}${azureKey ? `&subscription-key=${azureKey}` : ''
                    }`;
            case 'SectionalCharts':
                return 'http://localhost:8187/sectional/{z}/{x}/{-y}.png';
            case 'IFREnrouteLowCharts':
                return 'http://localhost:8187/ifr-enroute-low/{z}/{x}/{-y}.png';
            case 'IFREnrouteHighCharts':
                return 'http://localhost:8187/ifr-enroute-high/{z}/{x}/{-y}.png';
            case 'TerminalAreaCharts':
                return 'http://localhost:8187/terminal-area/{z}/{x}/{-y}.png';
            case 'HelicopterCharts':
                return 'http://localhost:8187/helicopter/{z}/{x}/{-y}.png';
        }
    };

    const layers = styles.map((name) => {

        const layer = new TileLayer({
            visible: false,
            preload: Infinity,
            source: new XYZ({ url: urlFor(name) }),
        });
        layer.set('name', name);

        return layer;

    });

    return { styles, layers };

}

export default function FlightsPanelMap(props: FlightsPanelMapProps) {

    const {
        initialStyle = 'RoadOnDemand',
        initialCenterLonLat = [-97.0329, 47.9253],
        initialZoom = 1,
    } = props;

    const azureKey: string | undefined = undefined; //<-- Azure Maps key

    const containerRef = useRef<HTMLDivElement | null>(null); //<-- Map target
    const popupRef = useRef<HTMLDivElement | null>(null); //<-- Overlay element
    const closerRef = useRef<HTMLAnchorElement | null>(null);
    const contentRef = useRef<HTMLDivElement | null>(null);

    const mapRef = useRef<Map | null>(null);
    const overlayRef = useRef<Overlay | null>(null);
    const layersRef = useRef<TileLayer<XYZ>[]>([]);
    const [mapError, setMapError] = useState(false);

    
    const setVisibleBase = (name: BaseStyleName) => {

        layersRef.current.forEach((layer) => {
            const lname = layer.get('name') as BaseStyleName | undefined;
            layer.setVisible(lname === name);
        });

    };


    useEffect(() => {

        // Container not ready, exit
        if (!containerRef.current)
            return;

        try {

            // Build layers
            const { layers } = createBaseMapLayers(azureKey);
            layersRef.current = layers;

            // Popup overlay
            if (popupRef.current) {
                overlayRef.current = new Overlay({
                    element: popupRef.current,
                    autoPan: true,
                });
            }

            // Create map
            const map = new Map({
                target: containerRef.current,
                layers,
                // loadTilesWhileInteracting: true,
                view: new View({
                    center: fromLonLat(initialCenterLonLat),
                    zoom: initialZoom,
                    maxZoom: 20,
                    minZoom: 0,
                }),
                overlays: overlayRef.current ? [overlayRef.current] : undefined,
            });

            mapRef.current = map;

            setVisibleBase(initialStyle);

            // Basic overlay close handler
            const closerElement = closerRef.current;
            const overlayElement = overlayRef.current;
            if (closerElement && overlayElement) {
                closerElement.onclick = (e) => {
                    e.preventDefault();
                    overlayElement.setPosition(undefined);
                };
            }

            map.on('singleclick', (evt) => {

                if (!overlayElement || !contentRef.current)
                    return;

                const coord3857 = evt.coordinate;

                contentRef.current.innerHTML = `<p><strong>Clicked:</strong> [${coord3857
                    .map((v) => v.toFixed(2))
                    .join(', ')}]</p>`;

                overlayElement.setPosition(coord3857);

            });

        } catch (error) {

            log.error('Failed to initialize OpenLayers map:', error);
            setMapError(true);

        }

        return () => {

            try {
                overlayRef.current?.setPosition(undefined);
                mapRef.current?.setTarget(undefined as unknown as HTMLElement); //<-- Detach safely
            } catch (error) {
                /* ... */
            }

            mapRef.current = null;
            overlayRef.current = null;
            layersRef.current = [];

        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); //<-- Init once


    useEffect(() => {

        // Key is set, all good
        if (azureKey)
            return;

        // Azure key is missing, show warning
        log.error('Azure Maps key is missing or undefined!');
        setMapError(true);

    }, [azureKey]);

    const renderMapUnavailable = () => {

        return <div className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center text-destructive">
            <AlertCircle className="" />

            <div className="flex flex-col">
                <AlertTitle>Map Unavailable!</AlertTitle>
                <AlertDescription>
                    The selected map type has failed to load.
                    <br />
                    Please try again later.
                </AlertDescription>
            </div>
        </div>

    }


    const render = () => {

        log('Rendering FlightsPanelMap, mapError=', mapError);

        const mapContainerClass = `bg-background h-full w-full ${mapError ? 'hidden!' : ''}`;

        return (
            <Card className="border rounded-lg w-full h-full card-glossy relative @container">
                <motion.div layoutScroll className="flex-1 min-h-0 h-full w-full overflow-y-auto">

                    {/* Overlay */}
                    <div
                        ref={popupRef}
                        id="popup"
                        className="ol-popup rounded-xl shadow-lg bg-background/95 border p-3"
                        style={{ display: 'none' }}
                        onTransitionEnd={() => {
                            if (popupRef.current && overlayRef.current?.getPosition())
                                popupRef.current.style.display = 'block';
                        }}
                    >
                        <a
                            ref={closerRef}
                            id="popup-closer"
                            href="#"
                            className="ol-popup-closer text-muted-foreground"
                            aria-label="Close popup"
                        >
                            ×
                        </a>
                        <div ref={contentRef} id="popup-content" />
                    </div>

                    {/* Map container */}
                    <div ref={containerRef} id="map" className={mapContainerClass} />

                    {/* Map Error Message */}
                    {
                        (mapError)
                        &&
                        <div className="min-h-full flex items-center justify-center p-6">
                            {renderMapUnavailable()}
                        </div>
                    }
                    
                </motion.div>
            </Card>
        );

    }

    return render();

}