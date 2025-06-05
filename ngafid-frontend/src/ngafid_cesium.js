import React from "react";
import {
    Cartesian3,
    Entity,
    VelocityOrientationProperty,
    Ion, Math, IonResource,
    JulianDate,
    TimeIntervalCollection,
    TimeInterval,
    SampledPositionProperty,
    PathGraphics, Color,
    ModelGraphics,
    PolylineOutlineMaterialProperty, CornerType
} from "cesium";
import {Viewer, Scene, Globe, Clock, SkyAtmosphere} from "resium";


class CesiumPage extends React.Component {
    
    constructor(props) {

        super(props);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3OTU3MDVjYS04ZGRiLTRkZmYtOWE5ZC1lNTEzMTZlNjE5NWEiLCJpZCI6OTYyNjMsImlhdCI6MTY1NDMxNTk2N30.c_n2k_FWWisRoXnAFVGs6Nbxk0NVFmrIpqL12kjE7sA";
        Math.setRandomNumberSeed(9);
        this.state = {
            modelURL: null,
            flightColors : {},
            positionProperty: {},
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {},
            activePhaseEntities : {},
            activeEventEntites : {},
            flightData : {},
            currentZoomedEntity : null
        };
        this.loadModel();
    }

    async loadModel() {

        const airplaneURI = await IonResource.fromAssetId(1084423);
        const droneURI = await IonResource.fromAssetId(1117220);

        const newAirFrameModels = {
            ...this.state.airFrameModels,
            "Airplane": new ModelGraphics({
                uri: airplaneURI,
                minimumPixelSize: 64,
                maximumScale: 20000,
                scale: 0.5,
            }),
            "Drone": new ModelGraphics({
                uri: droneURI,
                minimumPixelSize: 64,
                maximumScale: 20000,
                scale: 0.5,
            })
        };

        this.setState({ airFrameModels: newAirFrameModels });
    }

    getPositionProperty(flightData) {
       
        console.log(flightData);
        const positionProperty = new SampledPositionProperty();
        const infoAglDemo = flightData["flightGeoInfoAgl"];
        console.log("infoagldemo");
        console.log(infoAglDemo);
        const infAglDemoTimes = flightData["flightAglTimes"];
        const positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);

        for (let i = 0; i < positions.length; i++ ) {
            positionProperty.addSample(JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }

        return positionProperty;
    }

    getCesiumData(flightId) {

        let cesiumData = null;
        const submissionData = {
            "flightId" : flightId
        };

        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            traditional : true,
            data : submissionData,
            dataType : 'json',
            async: false,
            success: (response) => {
                console.log(response);
                cesiumData = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(errorThrown);
            },
        });

        return cesiumData;
    }

    getFlightKeepGroundEntity(type, data) {
        
        const positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        const colorMap = {
            "default": Color.LEMONCHIFFON,
            "Taxiing": Color.BLUE,
            "Takeoff": Color.PURPLE,
            "Climb": Color.SADDLEBROWN,
            "Cruise": Color.MEDIUMSEAGREEN,
        };

        const entity = new Entity({

           /*  name: "NGAFID CESIUM FLIGHT TAXIING ",
            description: "<a><h3> NGAFID Flight ID: " + keys[i] + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Taxiing</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
  */        polyline: {
                positions: positionsArr,
                width: 1,
                material: new Cesium.PolylineOutlineMaterialProperty({
                    color: colorMap[type],
                    outlineWidth: 2,
                    outlineColor: Color.BLACK,
                }),
                clampToGround: true,
            },
        });
        
        return entity;
    }

    getLoadAGLEntity(type, data, flightId) {

        const positionArr = Cartesian3.fromDegreesArrayHeights(data);
        const colorMap = {
            "default" : Color.WHITESMOKE.withAlpha(0.8),
            "Taxiing" : Color.BLUE.withAlpha(0.8),
            "Takeoff" : Color.BLUE.withAlpha(0.8),
            "Climb" : Color.SADDLEBROWN.withAlpha(0.8),
            "Cruise" : Color.LIGHTCYAN.withAlpha(0.8),
            "FullFlight" : Color.LIGHTCYAN.withAlpha(0.8)
        };
        const today = new Date();
        console.log(colorMap[type]);

        const entity = new Entity({

            name : `NGAFID CESIUM : ${  type  } ${  flightId}`,
            description: (
                <a>
                    <h3>NGAFID Flight ID: {flightId}</h3>
                    <p>Reanimation of Flight - {type}</p>
                    <p>Displaying Time: {today.toLocaleString()}</p>
                </a>
            ),
            wall: {
                    positions: positionArr,
                    material: colorMap[type],
                    cornerType: CornerType.BEVELED,
                }
        });
        return entity;
    }

    getFlightLineEntity(type, data) {

        const positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        const colorMap = {
            "default" : Color.BLUE,
            "Taxiing" : Color.BLUE,
            "Takeoff" : Color.PURPLE,
            "Climb": Color.SADDLEBROWN,
            "Cruise": Color.MEDIUMSEAGREEN,
        };
        const entity = new Entity({

            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
  */        polyline: {
                positions: positionsArr,
                width: 3,
                material: new Cesium.PolylineOutlineMaterialProperty({
                    color: colorMap[type],
                    outlineWidth: 2,
                    outlineColor: Cesium.Color.BLACK,
                }),
            },
        });

        return entity;
    }
 
    addPhaseEntity(phase, flightId) {

        let positionKey = "";
        let positionArr = null;
        
        console.log("Adding phase entity for: ", phase);
       
        const keyMap = {
            "Taxiing" : "flightGeoAglTaxiing",
            "Takeoff" : "flightGeoAglTakeOff",
            "Climb" : "flightGeoAglClimb",
            "Cruise to Final" : "flightGeoAglCruise",
            "Full Flight" : "flightGeoInfoAgl"
        };

        //Get the position key for the phase
        positionKey = keyMap[phase];

        //Get the position array for the phase
        positionArr = this.state.flightData[flightId][positionKey];

        //Phase is already active, remove the entities
        if (phase in this.state.activePhaseEntities[flightId]) {
            
            for (const entity of this.state.activePhaseEntities[flightId][phase]) {
                this.viewer.entities.remove(entity);
            }
            delete this.state.activePhaseEntities[flightId][phase];

        //Phase is not active, add the entities
        } else {
            const geoFlightLoadAgl = this.getLoadAGLEntity(phase, positionArr, flightId);
            const geoFlightLineTaxi = this.getFlightLineEntity(phase, positionArr);
            const geoFlightKeepGround = this.getFlightKeepGroundEntity(phase, positionArr, flightId);
            this.viewer.entities.add(geoFlightLoadAgl);
            this.viewer.entities.add(geoFlightLineTaxi);
            this.viewer.entities.add(geoFlightKeepGround);

            const updatedActivePhaseEntities = { ...this.state.activePhaseEntities };
            if (!updatedActivePhaseEntities[flightId])
                updatedActivePhaseEntities[flightId] = {};

            updatedActivePhaseEntities[flightId][phase] = [geoFlightLoadAgl, geoFlightLineTaxi, geoFlightKeepGround];
            this.setState({ activePhaseEntities: updatedActivePhaseEntities });
        }
        
        //Trigger state update
        this.setState(this.state);
        
    }

    getEventEntity(event, flightId ,data, color) {



        //adding altitude to event entity so color is visible
        for(let i = 2; i < data.length; i+=3) {
            data[i] += 5;
        }
        const flightData = this.state.flightData[flightId];
        const positionProperty = new SampledPositionProperty();
        const infoAglDemo = flightData["flightGeoInfoAgl"];
        const infAglDemoTimes = flightData["flightAglTimes"];
        const positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);
        console.log(event.startLine);
        for (let i = event.startLine - 3; i < positions.length; i++ ) {
            positionProperty.addSample(JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }

        console.log(positionProperty);

        const model = this.state.airFrameModels["Airplane"];
        const flightEndTime = JulianDate.fromIso8601(this.state.flightData[flightId].endTime);
        const eventStartTime = JulianDate.fromIso8601(infAglDemoTimes[event.startLine - 3]);
        console.log(model);
        console.log(eventStartTime);
        console.log(flightEndTime);
        const positionArr = Cartesian3.fromDegreesArrayHeights(data);
        const pathColor = Color.fromCssColorString(color).withAlpha(1);
        const entity = new Entity({
            
            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            */
            polyline: {
                positions: positionArr,
                width: 8,
                material: new Cesium.PolylineOutlineMaterialProperty({
                    color: pathColor,
                    outlineWidth: 7,
                    outlineColor: pathColor,
                }),
                // clampToGround: true,
                // zIndex:2
            },
            
        });

        return entity;
    }

    getDefaultLineEntity(data, color) {

        const positionArr = Cartesian3.fromDegreesArrayHeights(data);
        const entity = new Entity({
            
            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            */
            polyline: {
                positions: positionArr,
                width: 3,
                material: new Cesium.PolylineOutlineMaterialProperty({
                    color: color,
                    outlineWidth: 2,
                    outlineColor: color,
                }),
                // clampToGround: true,
                // zIndex: 1
            },
            
        });

        return entity;
    }

    addEventEntity(event, flightId) {
        
        if (event.id in this.state.activeEventEntites) {
            
            const eventEntity = this.state.activeEventEntites[event.id];
            this.viewer.entities.remove(eventEntity);
            delete this.state.activeEventEntites[event.id];

        } else {
            const infoAgl = this.state.flightData[flightId].flightGeoInfoAgl;
            const eventStartLine = event.startLine*3 - 3;
            const eventEndLine = event.endLine*3 + 3;
            console.log(`Start line : ${  eventStartLine  } End Line : ${  eventEndLine}`);
            const eventCoordinates = infoAgl.slice(eventStartLine, eventEndLine);
            const entity = this.getEventEntity(event, flightId, eventCoordinates, event.color); 
            this.viewer.entities.add(entity);
            const updatedActiveEventEntites = { ...this.state.activeEventEntites, [event.id]: entity };
            this.setState({ activeEventEntites: updatedActiveEventEntites });
            console.log(event);
        }

        this.setState(this.state);
    }

    removeFlightEntities(flightId) {
        console.log(this.state.activePhaseEntities[flightId]); 
        for (const phase in this.state.activePhaseEntities[flightId]) {
            console.log(phase);
            const entities = this.state.activePhaseEntities[flightId][phase];
            for (const entity of entities) {
                console.log(entity);
                this.viewer.entities.remove(entity);
            }
        }

        for (const eventId in this.state.activeEventEntites) {
            this.viewer.entities.remove(this.state.activeEventEntites[eventId]);
        }

        delete this.state.activePhaseEntities[flightId];
        delete this.state.activeEventEntites[flightId];
        this.setState(this.state);
        this.zoomToEntity();

    }

    cesiumFlightTrackedSet(flightId) {

        console.log(`cesiumFlightTrackedSet -- Toggling camera for flight: ${  flightId}`);
        
        //Clear current tracked entity form the viewer first
        this.viewer.trackedEntity = null;

        //Flight found in activePhaseEntities, set the tracked entity to the flight entity
        if (flightId in this.state.activePhaseEntities) {

            const flightReplayEntity = this.state.activePhaseEntities[flightId]["default"][0];
            this.viewer.trackedEntity = flightReplayEntity;

        //Flight not found in activePhaseEntities, log error
        } else {
            console.log("cesiumFlightTrackedSet -- Flight not found in activePhaseEntities: ", flightId);
        }

    }

    zoomToEntity() {

        if (Object.keys(this.state.activePhaseEntities) != 0) {
            
            const flightId = Object.keys(this.state.activePhaseEntities)[0];
            this.viewer.zoomTo(this.state.activePhaseEntities[flightId]["default"][1]);
            
        }
    }
    
    zoomToEventEntity(eventId, flightId) {

        const eventEntity = this.state.activeEventEntites[eventId];

        console.log(`Zooming to event entity: (Event ID: ${eventId}, Flight ID: ${flightId})`);

        this.viewer.zoomTo(eventEntity);
        this.setState({ currentZoomedEntity: eventEntity });
    }

    addDefaultEntities(flightId, color) {

        const flightData = this.state.flightData[flightId];
        const flightStartTime = JulianDate.fromIso8601(flightData.startTime);
        const flightEndTime = JulianDate.fromIso8601(flightData.endTime);
        console.log("Flight start time : ");
        console.log("Before entities");
        console.log(this.viewer.entities);
        console.log(flightStartTime);
        if (JulianDate.compare(flightStartTime, this.viewer.clock.startTime) < 0) {
            // console.log("Earlier time detected. Setting start time to " + startTime)
            this.viewer.clock.startTime = flightStartTime.clone();
            this.viewer.clock.currentTime = flightStartTime.clone();
        }

        if (JulianDate.compare(this.viewer.clock.stopTime, flightEndTime) > 0) {
            // console.log("Later time detected. Setting end time to " + endTime);
            this.viewer.clock.stopTime = flightEndTime.clone();

        }

        this.viewer.clock.shouldAnimate = true;
        this.viewer.clock.multiplier = 10;
        this.viewer.timeline.zoomTo(flightStartTime.clone(), flightEndTime.clone());
        const pathColor = Color.fromCssColorString(color).withAlpha(0.8);
        const positionProperty = this.getPositionProperty(flightData);

        let model = null;
        if (flightData["airframeType"] == "Fixed Wing" || flightData["airframeType"] == "UAS Fixed Wing") {
            model = this.state.airFrameModels["Airplane"];
        }
        else if (flightData["airframeType"] == "UAS Rotorcraft") {
            model = this.state.airFrameModels["Drone"];
        }

        const replayEntity = new Entity({
            availability: new TimeIntervalCollection([new TimeInterval({start: flightStartTime, stop: flightEndTime})]),
            position: positionProperty,
            orientation: new VelocityOrientationProperty(positionProperty),
            model: model,
            
            path: new PathGraphics({
                    width: 5,
                    material: new PolylineOutlineMaterialProperty({
                        color: pathColor,
                        outlineColor: pathColor,
                        outlineWidth: 5
                })
            }), 
        });
        const infoAglDemo = this.state.flightData[flightId]["flightGeoInfoAgl"];
        this.setState(prevState => ({
            flightColors: {
                ...prevState.flightColors,
                [flightId]: color
            }
        }));
        const geoFlightGroundEntity = this.getFlightKeepGroundEntity("default", infoAglDemo);

        const updatedActivePhaseEntities = { ...this.state.activePhaseEntities };
        updatedActivePhaseEntities[flightId] = { "default": [replayEntity, geoFlightGroundEntity] };

        this.viewer.entities.add(geoFlightGroundEntity);
        this.viewer.entities.add(replayEntity);
        this.viewer.zoomTo(geoFlightGroundEntity);

        this.setState({
            activePhaseEntities: updatedActivePhaseEntities,
            currentZoomedEntity: geoFlightGroundEntity
        });

    }

    addFlightEntity(flightId, color) {
        
        console.log("Cesium flight color: ", color);
        if (flightId in this.state.activePhaseEntities) {

            console.log("Removing flight from cesium");
            this.removeFlightEntities(flightId);

        } else {

            console.log("Adding flight to cesium");
            const cesiumData = this.getCesiumData(flightId)[flightId];
            this.setState(prevState => ({
                flightData: {
                    ...prevState.flightData,
                    [flightId]: cesiumData
                }
            }), () => {
                this.addDefaultEntities(flightId, color);
            });
            
        }
               
    }

    componentDidUpdate(prevProps) {
        
        //Change in resolution scale
        if (prevProps.cesiumResolutionScale !== this.props.cesiumResolutionScale) {

            console.log(`Cesium resolution scale updated: ${  this.props.cesiumResolutionScale}`);

            this.viewer.resolutionScale = this.props.cesiumResolutionScale;
            this.viewer.resize();
        }

        //Change in use of browser recommended resolution
        if (prevProps.cesiumResolutionUseDefault !== this.props.cesiumResolutionUseDefault) {

            console.log(`Cesium resolution use default updated: ${  this.props.cesiumResolutionUseDefault}`);

            this.viewer.useBrowserRecommendedResolution = this.props.cesiumResolutionUseDefault;
            this.viewer.resize();
        }

    }


    cesiumJumpToFlightStart(flightId) {

        /*
            Set the Cesium Viewer playhead position
            to the start of the selected flight
        */
    
        console.log(`Jumping to flight start: ${  flightId}`);
    
        const flightData = this.state.flightData;
        const viewer = this.viewer;
    
        //Get the flight start time
        const flightStartTime = JulianDate.fromIso8601(flightData[flightId].startTime);
        const flightEndTime = JulianDate.fromIso8601(flightData[flightId].endTime);
    
        //Set the viewer clock start time to the flight start time
        viewer.clock.startTime = flightStartTime.clone();
        viewer.clock.currentTime = flightStartTime.clone();
        viewer.clock.shouldAnimate = true;
        viewer.timeline.zoomTo(flightStartTime.clone(), flightEndTime.clone());
    
    }

    render() {

           return (
                <div style={{width:'100%', height: '100%' }}>
                    <div id="cesiumContainer"> 

                        <Viewer full
                                ref={e => {
                                    this.viewer = e ? e.cesiumElement : undefined;
                                }}
                                timeline={true}
                                scene3DOnly={true}
                                selectionIndicator={true}
                                baseLayerPicker={false}
                                orderIndependentTranslucency={false}
                                style={{
                                    position: "relative",
                                }}
                                useBrowserRecommendedResolution = {this.props.cesiumResolutionUseDefault}
                                resolutionScale={this.props.cesiumResolutionScale}
                        >
                            <Clock/>
                            <Scene
                                ref={e => {
                                    this.scene = e ? e.cesiumElement : undefined;
                                }}
                            />
                            <SkyAtmosphere
                                hueShift={0.0}
                                saturationShift={0.0}
                                brightnessShift={0.0}
                            />
                            <Globe
                                depthTestAgainstTerrain = {true}
                                atmosphereHueShift={0.0}
                                atmosphereSaturationShift={0.0}
                                atmosphereBrightnessShift={0.0}
                            />
                        </Viewer>
                    </div>
                </div>
        );
    };
}


const ForwardedCesiumPage = (props => <CesiumPage ref={props.setRef} {...props} />);
ForwardedCesiumPage.displayName = "ForwardedCesiumPage";
export default ForwardedCesiumPage;