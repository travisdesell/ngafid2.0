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
            flightColors : {},
            positionProperty: {},
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {},
            activePhaseEntities : {},
            activeEventEntites : {},
            flightData : {},
            activeEntities : {}
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
                material: Color.WHITESMOKE.withAlpha(0.8),
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

        for (let i = 0; i < positionArr.length; i+=3) {
            var geoFlightLoadAgl = this.getLoadAGLEntity(phase, positionArr.slice(i, i+3)
            , flightId);

        }

        var geoFlightLineTaxi = this.getFlightLineEntity(phase, positionArr);
        var geoFlightKeepGround = this.getFlightKeepGroundEntity(phase, positionArr, flightId);
        
        this.viewer.entities.add(geoFlightLoadAgl);
        this.viewer.entities.add(geoFlightLineTaxi);
        this.viewer.entities.add(geoFlightKeepGround);

        this.state.activePhaseEntities[phase] = [geoFlightLoadAgl, geoFlightKeepGround, geoFlightLineTaxi];
        this.setState(this.state);
        
    }

    getEventEntity(data, color) {
        
        var positionArr = Cartesian3.fromDegreesArrayHeights(data);
        var pathColor = Color.fromCssColorString(color).withAlpha(0.999);
        var entity = new Entity({
            
            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            */
            polyline: {
            positions: positionArr,
            width: 8,
            material: new Cesium.PolylineOutlineMaterialProperty({
                color: pathColor,
                outlineWidth: 5,
                outlineColor: pathColor,
                }),
                zIndex:2
            
            },
            
        });

        return entity;
    }

    getDefaultLineEntity(data, color) {

        var positionArr = Cartesian3.fromDegreesArrayHeights(data);
        var pathColor = Color.fromCssColorString(color);
        var entity = new Entity({
            
            /* name: "NGAFID CESIUM FLIGHT " + type,
            description: "<a><h3> NGAFID Flight ID: " +  + "</h3> </a>" + " <hr> <p> Reanimation of Flight - Climb</p>" + "<hr> <p> Displaying Time: " + today + "</p>",
            */
            polyline: {
            positions: positionArr,
            width: 3,
            material: new Cesium.PolylineOutlineMaterialProperty({
                color: pathColor,
                outlineWidth: 2,
                outlineColor: pathColor,
                }),
            
            },
            
        });

        return entity;
    }

    addEventEntity(event, flightId) {
        console.log("In cesium : ");
        console.log(event);
        console.log(flightId);
        console.log(this.state.flightData[flightId]);
        var infoAgl = this.state.flightData[flightId].flightGeoInfoAgl;
        var eventStartLine = event.startLine*3  ;
        var eventEndLine = event.endLine*3;
        console.log("Start line : " + eventStartLine + " End Line : " + eventEndLine);
        var eventCoordinates = infoAgl.slice(eventStartLine, eventEndLine);
        console.log(eventCoordinates);

        var entity = this.getEventEntity(eventCoordinates, event.color); 
       
        // var activeEntites = this.state.activeEntities;
/*         for (const [key, value] of Object.entries(activeEntites)) {

            console.log(typeof(key));
            var startLine = parseInt(key.split(",")[0]);
            var endLine = parseInt(key.split(",")[1])
            // var [startLine, endLine] = key;
            console.log("Start line : " + startLine + " Endline : " + endLine);
            console.log("Event Start  : " + eventStartLine + " end : " + eventEndLine);
            if ( startLine < eventStartLine && endLine > eventEndLine) {
                
                console.log("in if loop");
                
                var newPositionArr = infoAgl.slice(startLine, eventStartLine);
                var newPositionArr2 = infoAgl.slice(eventEndLine, endLine);
                

                var entity1 = this.getDefaultLineEntity(newPositionArr, this.state.flightColors[flightId]);
                var entity2 = this.getDefaultLineEntity(newPositionArr2, this.state.flightColors[flightId]);
                
                
                activeEntites[[eventStartLine, eventEndLine]] = entity;
                activeEntites[[startLine, eventEndLine]] = entity1;
                activeEntites[[eventEndLine, endLine]] = entity2;

                this.viewer.entities.remove(value);
                
                this.viewer.entities.add(entity);
                this.viewer.entities.add(entity1);
                this.viewer.entities.add(entity2);
                delete activeEntites[key];
                break;

            }
            
        } 
 */
        // this.state.activeEntities = activeEntites;
        // this.setState(this.state);
 
        this.viewer.entities.add(entity);
        var eventTime = JulianDate.fromIso8601(event.startTime);
        this.viewer.clock.currentTime = eventTime;
        
        this.viewer.zoomTo(entity);
    }

    addDefaultEntities(flightId, color) {
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
        var pathColor = Color.fromCssColorString(color);
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
            
            /* path: new PathGraphics({
                    width: 5,
                    material: new PolylineOutlineMaterialProperty({
                        color: pathColor,
                        outlineColor: pathColor,
                        outlineWidth: 5
                })
            }),  */
            
        });
        this.state.activePhaseEntities[flightId] = { "replayEntity" : replayEntity };
        var infoAglDemo = this.state.flightData[flightId]["flightGeoInfoAgl"];
        var defaultEntity = this.getDefaultLineEntity(infoAglDemo, pathColor);
        this.state.flightColors[flightId] = color;
        this.setState(this.state);
        var geoFlightGroundEntity = this.getFlightKeepGroundEntity("default", infoAglDemo);
        this.state.activePhaseEntities[flightId]["groundEntity"] = geoFlightGroundEntity;
        this.viewer.entities.add(geoFlightGroundEntity);
        this.viewer.entities.add(entity);
        // this.viewer.zoomTo(entity);
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

    addFlightEntity(flightId, entityType, color) {
        
        console.log("Cesium flight color : " + color);
        if (!(flightId in this.state.flightData)) {
            this.state.flightData[flightId] = this.getCesiumData(flightId)[flightId];
            this.setState(this.state);
        }
        if (entityType == "default") {
            this.addDefaultEntities(flightId, color);
        } 
       
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
                                orderIndependentTranslucency={false}

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
