import ReactDOM from "react-dom";
import React from "react";
import {
    Cartesian3,
    Entity,
    Ion, Math, IonResource,
     JulianDate,
    TimeIntervalCollection,
    TimeInterval,
    PathGraphics, Color,
    PolylineOutlineMaterialProperty, PolylineGraphics, CornerType
} from "cesium";
import {Viewer, Scene, Globe, Clock, SkyAtmosphere, ModelGraphics, Model} from "resium";


class CesiumPage extends React.Component {
    //TODO Load in flight_component page
    //TODO Fix zoom issues
    //TODO Get plane and dji model
    //TODO add and remove entities
    //TODO skip to event time
    constructor(props) {

        super(props);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJiNzg1ZDIwNy0wNmRlLTQ0OWUtOTUwZS0zZTI4OGM0NTFlODIiLCJpZCI6MTYyNDM4LCJpYXQiOjE2OTI5MDc0MzF9.ZtqAnFch5mkZWLZdmNY2Zh-pNH_-XhUPhMrBZSsxyjw";
        Math.setRandomNumberSeed(9);
        this.state = {
            modelURL: null,
            positionProperty: {},
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {},
            activeEntities : {},
            flightData : {}
        };
    }

    async loadModel(airplaneType) {
        var url = null;
        if (airplaneType == "Fixed Wing" || airplaneType == "UAS Fixed Wing") {
            await IonResource.fromAssetId(1084423).then().then(function (value) {
                console.log(value._ionEndpointResource);
                url = value._ionEndpointResource;
            });
        }
        else if (airplaneType == "UAS Rotorcraft") {
            await IonResource.fromAssetId(1117220).then().then(function (value) {
                console.log(value._ionEndpointResource);
                url = value._ionEndpointResource;
            });
        }

        return url;
    }

    getPositionProperty(flightId, flightData) {
        
        var positionProperty = new Cesium.SampledPositionProperty();
        var infoAglDemo = flightData[flightId]["flightGeoInfoAgl"];
        console.log("infoagldemo");
        console.log(infoAglDemo);
        var infAglDemoTimes = flightData[flightId]["flightAglTimes"];
        var positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);
        for (let i = 0; i < positions.length; i++ ) {
            positionProperty.addSample(Cesium.JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
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
        /* let entity = (
            <Entity
                name={"NGAFID CESIUM : " + type + " " + flightId}
                description={"<a><h3> NGAFID Flight ID: " + flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight - " + type +
                    "</p>" + "<hr> <p> Displaying Time: " + today + "</p>"}
                wall={
                    {
                        positions : detailsMap[type],
                        material : detailsMap[type],
                        cornerType : CornerType.BEVELED
                    }
                }
            >
            </Entity>
        );
 */
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

       /*  let entity = (
            <Entity
                name={"NGAFID CESIUM FLIGHT  " + type}
                description={"<a><h3> NGAFID Flight ID: " + this.state.flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight -" + type  + "</p>" + "<hr> <p> Displaying Time: " + today + "</p>"}
                polyline={new PolylineGraphics({
                    positions: positionsArr,
                    width: 3,
                    material: new PolylineOutlineMaterialProperty({
                        color: colorMap[type].color,
                        outlineColor: Color.BLACK,
                        outlineWidth:2
                    })
                })}
            >
            </Entity>
        ); */

        return entity;
    }
 
    testClick() {
        console.log("test click from parent component");
    }

    addFlightEntity(flightId) {
        
        var flightData = this.getCesiumData(flightId);
        this.state.flightData[flightId] = {"data" : flightData};
        console.log(flightData.startTime);
        var clockStartTime = JulianDate.fromIso8601("9999-12-31T00:00:00");
        var clockEndTime = JulianDate.fromIso8601("0000-01-01T00:00:00");
        var flightStartTime = JulianDate.fromIso8601(flightData[flightId].startTime);
        var flightEndTime = JulianDate.fromIso8601(flightData[flightId].endTime);
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
        this.viewer.clock.startTime = clockStartTime;
        this.viewer.clock.stopTime = clockEndTime;
        this.viewer.clock.currentTime = clockStartTime;
        var infoAglDemo = flightData[flightId].flightGeoInfoAgl;
        var pathColor = Color.fromRandom();
        var positionProperty = this.getPositionProperty(flightId, flightData);
        /* var defaultEntity = new Entity({
                availability: new Cesium.TimeIntervalCollection([new Cesium.TimeInterval({start: clockStartTime, stop: clockEndTime})]),
                position: positionProperty,
                orientation: new Cesium.VelocityOrientationProperty(positionProperty),
                path: new Cesium.PathGraphics({
                        width: 5,
                        material: new Cesium.PolylineOutlineMaterialProperty({
                            color: pathColor,
                            outlineColor: pathColor,
                            outlineWidth: 5
                        })
                    }),
                point: {
                    pixelSize: 10,
                    color: Color.RED
                    },
        }); */

        var geoFlightAGL = this.getLoadAGLEntity("default", infoAglDemo, flightId);
        var geoGroundEntity = this.getFlightKeepGroundEntity("default", infoAglDemo, flightId);
        var geoFlightLineEntity = this.getFlightLineEntity("default", infoAglDemo, flightId);

        this.viewer.entities.add(geoGroundEntity);
        this.viewer.entities.add(geoFlightAGL);
        this.viewer.entities.add(geoFlightLineEntity);
        this.setState(this.state);
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
                            <Clock
                                startTime={clockStartTime}
                                stopTime={clockEndTime}
                                currentTime={clockStartTime}
                                shouldAnimate={true}
                                multiplier={25}
                            >
                            </Clock>
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
