import ReactDOM from "react-dom";
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
    PolylineOutlineMaterialProperty, PolylineGraphics, CornerType, PolylineGeometry, Primitive
} from "cesium";
import {Viewer, Scene, Globe, Clock, SkyAtmosphere} from "resium";
import { watch } from "fs";


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

        var airplaneURI = await IonResource.fromAssetId(1084423);
        this.state.airFrameModels["Airplane"] = new ModelGraphics({
            uri: airplaneURI,
            minimumPixelSize: 64,
            maximumScale: 20000,
            scale: 0.5,
        });

        var droneURI = await IonResource.fromAssetId(1117220);
        this.state.airFrameModels["Drone"] = new ModelGraphics({
            uri: droneURI,
            minimumPixelSize: 64,
            maximumScale: 20000,
            scale: 0.5,
        });

        this.setState(this.state);
    }

    getPositionProperty(flightData) {
       
        console.log(flightData);
        var positionProperty = new SampledPositionProperty();
        var infoAglDemo = flightData["flightGeoInfoAgl"];
        console.log("infoagldemo");
        console.log(infoAglDemo);
        var infAglDemoTimes = flightData["flightAglTimes"];
        var positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);

        for (let i = 0; i < positions.length; i++ ) {
            positionProperty.addSample(JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }

        return positionProperty;
    }

    getCesiumData(flightId) {

        var cesiumData = null;
        var submissionData = {
            "flightId" : flightId
        };

        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            traditional : true,
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log(response)
                cesiumData = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
            },
            async: false
        });

        return cesiumData;
    }

    getFlightKeepGroundEntity(type, data, flightId) {
        
        let positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            "default": Color.LEMONCHIFFON,
            "Taxiing": Color.BLUE,
            "Takeoff": Color.PURPLE,
            "Climb": Color.SADDLEBROWN,
            "Cruise": Color.MEDIUMSEAGREEN,
        }

        let entity = new Entity({

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
        })
        
        return entity;
    }

    getLoadAGLEntity(type, data, flightId) {

        var positionArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            "default" : Color.WHITESMOKE.withAlpha(0.8),
            "Taxiing" : Color.BLUE.withAlpha(0.8),
            "Takeoff" : Color.BLUE.withAlpha(0.8),
            "Climb" : Color.SADDLEBROWN.withAlpha(0.8),
            "Cruise" : Color.LIGHTCYAN.withAlpha(0.8),
            "FullFlight" : Color.LIGHTCYAN.withAlpha(0.8)
        }
        let today = new Date();
        console.log(colorMap[type]);

        var entity = new Entity({

            name : "NGAFID CESIUM : " + type + " " + flightId,
            description: "<a><h3> NGAFID Flight ID: " + flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight - " + type +
                    "</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            wall: {
                    positions: positionArr,
                    material: colorMap[type],
                    cornerType: CornerType.BEVELED,
                }
        });
        return entity;
    }

    getFlightLineEntity(type, data) {

        let positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            "default" : Color.BLUE,
            "Taxiing" : Color.BLUE,
            "Takeoff" : Color.PURPLE,
            "Climb": Color.SADDLEBROWN,
            "Cruise": Color.MEDIUMSEAGREEN,
        };
        var entity = new Entity({

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
            
            for (let entity of this.state.activePhaseEntities[flightId][phase]) {
                this.viewer.entities.remove(entity);
            }
            delete this.state.activePhaseEntities[flightId][phase];

        //Phase is not active, add the entities
        } else {
            var geoFlightLoadAgl = this.getLoadAGLEntity(phase, positionArr, flightId);
            var geoFlightLineTaxi = this.getFlightLineEntity(phase, positionArr);
            var geoFlightKeepGround = this.getFlightKeepGroundEntity(phase, positionArr, flightId);
            this.viewer.entities.add(geoFlightLoadAgl);
            this.viewer.entities.add(geoFlightLineTaxi);
            this.viewer.entities.add(geoFlightKeepGround);
            this.state.activePhaseEntities[flightId][phase] = [geoFlightLoadAgl, geoFlightLineTaxi, geoFlightKeepGround];
        }
        
        //Trigger state update
        this.setState(this.state);
        
    }

    getEventEntity(event, flightId ,data, color) {



        //adding altitude to event entity so color is visible
        for(let i = 2; i < data.length; i+=3) {
            data[i] += 5;
        }
        var flightData = this.state.flightData[flightId];
        var positionProperty = new SampledPositionProperty();
        var infoAglDemo = flightData["flightGeoInfoAgl"];
        var infAglDemoTimes = flightData["flightAglTimes"];
        var positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);
        console.log(event.startLine);
        for (let i = event.startLine - 3; i < positions.length; i++ ) {
            positionProperty.addSample(JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }

        console.log(positionProperty);

        var model = this.state.airFrameModels["Airplane"];
        var flightEndTime = JulianDate.fromIso8601(this.state.flightData[flightId].endTime);
        var eventStartTime = JulianDate.fromIso8601(infAglDemoTimes[event.startLine - 3]);
        console.log(model);
        console.log(eventStartTime);
        console.log(flightEndTime);
        var positionArr = Cartesian3.fromDegreesArrayHeights(data);
        var pathColor = Color.fromCssColorString(color).withAlpha(1);
        var entity = new Entity({
            
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

        var positionArr = Cartesian3.fromDegreesArrayHeights(data);
        var entity = new Entity({
            
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
            
            var eventEntity = this.state.activeEventEntites[event.id];
            this.viewer.entities.remove(eventEntity);
            delete this.state.activeEventEntites[event.id];

        } else {
            var infoAgl = this.state.flightData[flightId].flightGeoInfoAgl;
            var eventStartLine = event.startLine*3 - 3  ;
            var eventEndLine = event.endLine*3 + 3;
            console.log("Start line : " + eventStartLine + " End Line : " + eventEndLine);
            var eventCoordinates = infoAgl.slice(eventStartLine, eventEndLine);
            var entity = this.getEventEntity(event, flightId, eventCoordinates, event.color); 
            this.viewer.entities.add(entity);
            this.state.activeEventEntites[event.id] = entity;  
            console.log(event);
        }

        this.setState(this.state);
    }

    removeFlightEntities(flightId) {
        console.log(this.state.activePhaseEntities[flightId]); 
        for (const phase in this.state.activePhaseEntities[flightId]) {
            console.log(phase);
            var entities = this.state.activePhaseEntities[flightId][phase];
            for (let entity of entities) {
                console.log(entity)
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

        console.log("cesiumFlightTrackedSet -- Toggling camera for flight: " + flightId);
        
        //Clear current tracked entity form the viewer first
        this.viewer.trackedEntity = null;

        //Flight found in activePhaseEntities, set the tracked entity to the flight entity
        if (flightId in this.state.activePhaseEntities) {

            var flightReplayEntity = this.state.activePhaseEntities[flightId]["default"][0];
            this.viewer.trackedEntity = flightReplayEntity;

        //Flight not found in activePhaseEntities, log error
        } else {
            console.log("cesiumFlightTrackedSet -- Flight not found in activePhaseEntities: ", flightId);
        }

    }

    zoomToEntity() {

        if (Object.keys(this.state.activePhaseEntities) != 0) {
            
            var flightId = Object.keys(this.state.activePhaseEntities)[0];
            this.viewer.zoomTo(this.state.activePhaseEntities[flightId]["default"][1]);
            
        }
    }
    
    zoomToEventEntity(eventId, flightId) {

        const eventEntity = this.state.activeEventEntites[eventId];

        console.log(`Zooming to event entity: (Event ID: ${eventId}, Flight ID: ${flightId})`);

        this.viewer.zoomTo(eventEntity);
        this.state.currentZoomedEntity = eventEntity;     

        this.setState(this.state);
    }

    addDefaultEntities(flightId, color) {

        var flightData = this.state.flightData[flightId];
        var clockStartTime = JulianDate.fromIso8601("9999-12-31T00:00:00");
        var clockEndTime = JulianDate.fromIso8601("0000-01-01T00:00:00");
        var flightStartTime = JulianDate.fromIso8601(flightData.startTime);
        var flightEndTime = JulianDate.fromIso8601(flightData.endTime);
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
        var pathColor = Color.fromCssColorString(color).withAlpha(0.8);
        var positionProperty = this.getPositionProperty(flightData);

        var model = null;
        if (flightData["airframeType"] == "Fixed Wing" || flightData["airframeType"] == "UAS Fixed Wing") {
            model = this.state.airFrameModels["Airplane"];
        }
        else if (flightData["airframeType"] == "UAS Rotorcraft") {
            model = this.state.airFrameModels["Drone"];
        }

        var replayEntity = new Entity({
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
        var infoAglDemo = this.state.flightData[flightId]["flightGeoInfoAgl"];
        this.state.flightColors[flightId] = color;
        var geoFlightGroundEntity = this.getFlightKeepGroundEntity("default", infoAglDemo);

        this.state.activePhaseEntities[flightId] = {"default" : [replayEntity, geoFlightGroundEntity]};
             
        this.viewer.entities.add(geoFlightGroundEntity);
        // this.viewer.entities.add(defaultEntity);
        this.viewer.entities.add(replayEntity);
        this.viewer.zoomTo(geoFlightGroundEntity);
        this.state.currentZoomedEntity = geoFlightGroundEntity;
        this.setState(this.state);
        // return replayEntity;
    }

    addFlightEntity(flightId, color) {
        
        console.log("Cesium flight color : " + color);
        if (flightId in this.state.activePhaseEntities) {
            console.log("Removing flight from cesium");
            this.removeFlightEntities(flightId);
        } else {
            console.log("Adding flight to cesium");
            this.state.flightData[flightId] = this.getCesiumData(flightId)[flightId];
            this.addDefaultEntities(flightId, color);
            this.setState(this.state);
        }
               
    }

    componentDidUpdate(prevProps) {
        
        //Change in resolution scale
        if (prevProps.cesiumResolutionScale !== this.props.cesiumResolutionScale) {

            console.log("Cesium resolution scale updated: " + this.props.cesiumResolutionScale);

            this.viewer.resolutionScale = this.props.cesiumResolutionScale;
            this.viewer.resize();
        }

        //Change in use of browser recommended resolution
        if (prevProps.cesiumResolutionUseDefault !== this.props.cesiumResolutionUseDefault) {

            console.log("Cesium resolution use default updated: " + this.props.cesiumResolutionUseDefault);

            this.viewer.useBrowserRecommendedResolution = this.props.cesiumResolutionUseDefault;
            this.viewer.resize();
        }

    }


    cesiumJumpToFlightStart(flightId) {

        /*
            Set the Cesium Viewer playhead position
            to the start of the selected flight
        */
    
        console.log("Jumping to flight start: " + flightId);
    
        let flightData = this.state.flightData;
        let viewer = this.viewer;
    
        //Get the flight start time
        let flightStartTime = JulianDate.fromIso8601(flightData[flightId].startTime);
        let flightEndTime = JulianDate.fromIso8601(flightData[flightId].endTime);
    
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


export default (props => <CesiumPage ref={props.setRef} {...props} />);
