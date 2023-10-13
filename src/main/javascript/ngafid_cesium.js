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
    PolylineOutlineMaterialProperty, PolylineGraphics, CornerType
} from "cesium";
import {Viewer, Scene, Globe, Clock, SkyAtmosphere} from "resium";
import { watch } from "fs";
import { Col } from "react-bootstrap";


class CesiumPage extends React.Component {
    //TODO Load in flight_component page
    //TODO Fix zoom issues
    //TODO add and remove entities
    //TODO skip to event time
    constructor(props) {

        super(props);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3OTU3MDVjYS04ZGRiLTRkZmYtOWE5ZC1lNTEzMTZlNjE5NWEiLCJpZCI6OTYyNjMsImlhdCI6MTY1NDMxNTk2N30.c_n2k_FWWisRoXnAFVGs6Nbxk0NVFmrIpqL12kjE7sA";
        Math.setRandomNumberSeed(9);
        this.state = {
            modelURL: null,
            positionProperty: {},
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {},
            activePhaseEntities : {},
            activeEventEntites : {},
            flightData : {}
        };
        this.loadModel();
    }

    removeEntity(entity) {
        this.viewer.entities.remove(entity);
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

    getPositionProperty(flightId, flightData) {
       
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
            "default": {
                color: Color.LEMONCHIFFON,
            },
            "Taxiing": {
                color: Color.BLUE,
            },
            "Takeoff": {
                color: Color.PURPLE,
            },
            "Climb": {
                color: Color.SADDLEBROWN,
            },
            "Cruise": {
                color: Color.MEDIUMSEAGREEN,
            }
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
        let detailsMap = {
            "default" : {
                material: Color.WHITESMOKE.withAlpha(0),
            },
            "Taxiing" : {
                material: Color.BLUE.withAlpha(0.8),
            },
            "Takeoff" : {
                material: Color.BLUE.withAlpha(0.8),
            },
            "Climb" : {
                material: Color.SADDLEBROWN.withAlpha(0.8),
            },
            "Cruise" : {
                material: Color.LIGHTCYAN.withAlpha(0.8),
            }
        }
        let today = new Date();

        var entity = new Entity({

            name : "NGAFID CESIUM : " + type + " " + flightId,
            description: "<a><h3> NGAFID Flight ID: " + flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight - " + type +
                    "</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            wall: {
                    positions: positionArr,
                    material: detailsMap[type],
                    cornerType: CornerType.BEVELED,
                }
        });
        return entity;
    }

    getFlightLineEntity(type, data) {

        let positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            default : {
                color: Color.BLUE,
            },
            Taxiing : {
                color: Color.BLUE,
            },
            Takeoff : {
                color: Color.PURPLE,
            },
            Climb: {
                color: Color.SADDLEBROWN,
            },
            Cruise: {
                color: Color.MEDIUMSEAGREEN,
            }
        };
        console.log(colorMap[type].color)
        var today = new Date();

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
 
    testClick() {
        console.log("test click from parent component");
    }

    addPhaseEntity( phase, flightId) {
        console.log("Adding phase " + phase + " flight id : " + flightId);
        console.log(this.state.flightData[flightId]);
        var positionArr = null;
        if (phase == "Show Taxiing") {
            phase = "Taxiing";
            positionArr = this.state.flightData[flightId].flightGeoAglTaxiing;
        } else if (phase == "Show Takeoff") {
            phase = "Takeoff"
            positionArr = this.state.flightData[flightId].flightGeoAglTakeOff;
        } else if (phase == "Show Climb") {
            phase = "Climb"
            positionArr = this.state.flightData[flightId].flightGeoAglClimb;
        } else if (phase == "Show Cruise to Final") {
            phase = "Cruise"
            positionArr = this.state.flightData[flightId].flightGeoAglCruise;
        } else if (phase == "Show Full Flight") {
            phase = "FullFlight"
            positionArr = this.state.flightData[flightId].flightGeoInfoAgl;
        }

        var geoFlightLoadAgl = this.getLoadAGLEntity(phase, positionArr, flightId);
        var geoFlightLineTaxi = this.getFlightLineEntity(phase, positionArr);
        var geoFlightKeepGround = this.getFlightKeepGroundEntity(phase, positionArr, flightId);
        
        this.viewer.entities.add(geoFlightLoadAgl);
        this.viewer.entities.add(geoFlightLineTaxi);
        this.viewer.entities.add(geoFlightKeepGround);

        this.state.activePhaseEntities[phase] = [geoFlightLoadAgl, geoFlightKeepGround, geoFlightLineTaxi];
        this.setState(this.state);
        
    }

    addEventEntity(event, flightId) {
        console.log("In cesium : ");
        console.log(event);
        console.log(flightId);
        console.log(this.state.flightData[flightId]);
        var infoAgl = this.state.flightData[flightId].flightGeoInfoAgl;
        var startLine = event.startLine*3  ;
        var endLine = event.endLine*3;
        console.log("Start line : " + startLine + " End Line : " + endLine);
        var eventCoordinates = infoAgl.slice(startLine, endLine);
        console.log(eventCoordinates);
        var positionArr = Cartesian3.fromDegreesArrayHeights(eventCoordinates);

        console.log(Color.fromRgba(event.color));
        var entity = new Entity({

            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            */
            polyline: {
            positions: positionArr,
            width: 5,
            material: new Cesium.PolylineOutlineMaterialProperty({
                color: Color.fromCssColorString(event.color),
                outlineWidth: 10,
                outlineColor: Color.fromCssColorString(event.color),
                }),
            },
        });
        
        this.viewer.entities.add(entity);
        var eventTime = JulianDate.fromIso8601(event.startTime);
        this.viewer.clock.currentTime = eventTime;
        
        // this.viewer.zoomTo(entity);
    }

    getFlightReplayEntity(flightId) {
        var flightData = this.state.flightData[flightId];
        console.log("flightdata");
        console.log(flightData);
        var clockStartTime = JulianDate.fromIso8601("9999-12-31T00:00:00");
        var clockEndTime = JulianDate.fromIso8601("0000-01-01T00:00:00");
        var flightStartTime = JulianDate.fromIso8601(flightData.startTime);
        var flightEndTime = JulianDate.fromIso8601(flightData.endTime);
        console.log("Flight start time : ");
        console.log("Before entities");
        console.log(this.viewer.entities);
        console.log(flightStartTime);
        if (JulianDate.compare(flightStartTime, clockStartTime) < 0) {
            // console.log("Earlier time detected. Setting start time to " + startTime)
            clockStartTime = flightStartTime.clone();
        }

        if (JulianDate.compare(clockEndTime, flightEndTime) > 0) {
            // console.log("Later time detected. Setting end time to " + endTime);
            clockEndTime = flightEndTime.clone();
        }

        this.viewer.clock.startTime = flightStartTime.clone();
        this.viewer.clock.stopTime = flightEndTime.clone();
        this.viewer.clock.currentTime = flightStartTime.clone();
        this.viewer.clock.multiplier = 25;
        this.viewer.clock.shouldAnimate = true;
        var pathColor = Color.fromRandom();
        var positionProperty = this.getPositionProperty(flightId, flightData);

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
        this.state.activePhaseEntities[flightId] = { "replayEntity" : replayEntity };
        var infoAglDemo = this.state.flightData[flightId]["flightGeoInfoAgl"]; 
        
        var geoFlightLoadAglEntity = this.getLoadAGLEntity("default", infoAglDemo);
        var geoFlightGroundEntity = this.getFlightKeepGroundEntity("default", infoAglDemo);
        this.state.activePhaseEntities[flightId]["groundEntity"] = geoFlightGroundEntity;
        this.viewer.entities.add(geoFlightGroundEntity);

        this.viewer.zoomTo(geoFlightLoadAglEntity);
        return replayEntity;
    }

    removeAllEntities(flightId) {
    
        for (const [phase, entity] of Object.entries(this.state.activePhaseEntities[flightId])) {
            this.viewer.entities.remove(entity);
        }

        for (const [event, entity] of Object.entries(this.state.activeEventEntites[flightId])) {
            this.viewer.entities.remove(entity);
        }
        console.log("flight id : " + flightId + " removed from cesium");
    }

    addFlightEntity(flightId, entityType) {
        
        var entity = null;
        if (!(flightId in this.state.flightData)) {
            this.state.flightData[flightId] = this.getCesiumData(flightId)[flightId];
            this.setState(this.state);
        }
        if (entityType == "default") {
            entity = this.getFlightReplayEntity(flightId);
        } 
       
        this.viewer.entities.add(entity);
        this.viewer.trackedEntity = null;
        this.viewer.zoomTo(entity); 
    }

    toggleCamera() {
        console.log(this.viewer);
        console.log(this.mainEntity);
        this.viewer.trackedEntity = null;

        console.log("Toggle Camera");
    }

    render() {

        var flightPhases = ["Select Flight Phase", "Show Taxiing", "Show Takeoff", "Show Climb", "Show Cruise to Final", "Show Full Flight"]
        var clockStartTime = Cesium.JulianDate.fromIso8601("9999-12-31T00:00:00");
        var clockEndTime = Cesium.JulianDate.fromIso8601("0000-01-01T00:00:00");

        /* this.state.allFlightIds.map((flightId) => {
            var flightStartTime = JulianDate.fromIso8601(cesiumData[flightId].startTime);
            var flightEndTime = JulianDate.fromIso8601(cesiumData[flightId].endTime);
            console.log("Flight start time : ");
            console.log(flightStartTime);
            if (JulianDate.compare(flightStartTime, clockStartTime) < 0) {
                // console.log("Earlier time detected. Setting start time to " + startTime)
                clockStartTime = flightStartTime.clone();
            }

            if (JulianDate.compare(clockEndTime, flightEndTime) > 0) {
                // console.log("Later time detected. Setting end time to " + endTime);
                clockEndTime = flightEndTime.clone();
            }
            var infoAglDemo = cesiumData[flightId].flightGeoInfoAgl;
            var pathColor = Color.fromRandom();
            console.log(this.state.positionProperty[flightId]);
            var defaultEntity = (
                <Entity
                    availability={new TimeIntervalCollection([new TimeInterval({start: flightStartTime, stop: flightEndTime})])}
                    position={this.state.positionProperty[flightId]}
                    orientation={new Cesium.VelocityOrientationProperty(this.state.positionProperty[flightId])}
                    path={new PathGraphics({
                            width : 5,
                            material : new PolylineOutlineMaterialProperty({
                                color : pathColor,
                                outlineColor: pathColor,
                                outlineWidth: 5
                            })
                        }
                    )}
                    point={{ pixelSize: 10 , color: Color.RED}}
                    tracked={true}
                >
                    <ModelGraphics
                        uri={"Cesium_Air"}
                        scale={1}
                        minimumPixelSize={128}
                        maximumScale={2000}
                    >
                    </ModelGraphics>
                </Entity>
            );
            this.state.currentEntities[flightId] = {};
            this.state.currentEntities[flightId]["defaultEntity"] = defaultEntity;
            entities.push(defaultEntity);
            let model = this.state.airFrameModels["Airplane"];
            let taxiing = cesiumData[flightId].flightGeoAglTaxiing;
            let takeOff = cesiumData[flightId].flightGeoAglTakeOff;
            let climb = cesiumData[flightId].flightGeoAglClimb;
            let cruise = cesiumData[flightId].flightGeoAglCruise;

            //default entities
                       this.state.currentEntities[flightId]["demoEntity"] = geoFlightLoadAGLEntireDemoEntity;
            entities.push(geoFlightLoadAGLEntireDemoEntity);
            let groundEnity = this.getFlightKeepGroundEntity("default", infoAglDemo, flightId);
            this.state.currentEntities[flightId]["groundEntity"] = groundEnity; 
            entities.push(groundEnity);

            console.log("Entities : ");
            console.log(entities);
            //taxiing entitites
            let geoFlightLoadAGLTaxiEntity = this.getLoadAGLEntity("Taxiing", taxiing, flightId);
            let geoFlightlineTaxiEntity = this.getFlightLineEntity("Taxiing", taxiing);
            let geoFlightKeepGroundTaxiEntity = this.getFlightKeepGroundEntity("Taxiing", taxiing);

            //takeoff entities
            let geoFlightLoadAGLTakeOffEntity = this.getLoadAGLEntity("Takeoff", takeOff, flightId);
            let geoFlightlineTakeOffEntity = this.getFlightLineEntity("Takeoff", takeOff);
            let geoFlightKeepGroundTakeOffEntity = this.getFlightKeepGroundEntity("Takeoff", takeOff);

            // climb entities
            let geoFlightLoadAGLClimbfEntity = this.getLoadAGLEntity("Climb", climb, flightId);
            let geoFlightlineClimbEntity = this.getFlightLineEntity("Climb", climb);
            let geoFlightKeepGroundClimbEntity = this.getFlightKeepGroundEntity("Climb", climb);

            let geoFlightLoadAGLCruisefEntity = this.getLoadAGLEntity("Cruise", cruise, flightId);
            let geoFlightlineCruiseEntity = this.getFlightLineEntity("Cruise", cruise);
            let geoFlightKeepGroundCruiseEntity = this.getFlightKeepGroundEntity("Cruise", cruise);
        }) */

        return (
                <div>
                    <div id="cesiumContainer">

                        <Viewer full
                                ref={e => {
                                    this.viewer = e ? e.cesiumElement : undefined;
                                }}
                                timeline={true}
                                scene3DOnly={true}
                                selectionIndicator={true}
                                baseLayerPicker={false}

                        >
                        <Clock/>
                           <Scene
                                ref={e => {
                                    this.scene = e ? e.cesiumElement : undefined;
                                }}
                            >
                            </Scene>
                            <SkyAtmosphere
                                hueShift={0.0}
                                saturationShift={0.0}
                                brightnessShift={0.0}
                            ></SkyAtmosphere>
                            <Globe
                                depthTestAgainstTerrain = {true}
                                atmosphereHueShift={0.0}
                                atmosphereSaturationShift={0.0}
                                atmosphereBrightnessShift={0.0}
                            >
                            </Globe>
                        </Viewer>
                    </div>
            </div>
        );
    };
}

export default (props => <CesiumPage ref={props.setRef} />)
