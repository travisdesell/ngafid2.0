import ReactDOM from "react-dom";
import React from "react";
import {
    Cartesian3,
    Ion, Math, IonResource,
     JulianDate,
    TimeIntervalCollection,
    TimeInterval,
    PathGraphics, Color,
    PolylineOutlineMaterialProperty, PolylineGraphics, CornerType
} from "cesium";
import {Viewer, Entity, Scene, Globe, Clock, SkyAtmosphere, ModelGraphics, Model} from "resium";
import * as Cesium from "cesium";
// import { Flight } from './flight_component.js';


class CesiumPage extends React.Component {
    //TODO Load in flight_component page
    //TODO Fix zoom issues
    //TODO Get plane and dji model
    //TODO add and remove entities
    //
    constructor(props) {

        console.log("in cesium page");
        console.log(props);
        super(props);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJiNzg1ZDIwNy0wNmRlLTQ0OWUtOTUwZS0zZTI4OGM0NTFlODIiLCJpZCI6MTYyNDM4LCJpYXQiOjE2OTI5MDc0MzF9.ZtqAnFch5mkZWLZdmNY2Zh-pNH_-XhUPhMrBZSsxyjw";
        Math.setRandomNumberSeed(9);
        // var cesiumData = this.getCesiumData(allFlightIds);
        this.state = {
            modelURL: null,
            positionProperty: {},
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {},
            currentEntities : {}
        };
        console.log(this.state.modelURL);
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
    getPositionProperty(flightId) {
        var positionProperty = new Cesium.SampledPositionProperty();
        var infoAglDemo = this.state.cesiumData[flightId]["flightGeoInfoAgl"];
        var infAglDemoTimes = this.state.cesiumData[flightId]["flightAglTimes"];
        var positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);
        for (let i = 0; i < positions.length; i++ ) {
            positionProperty.addSample(Cesium.JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }
        return positionProperty;
    }

    componentDidMount() {
        console.log("in will mount");
        // var flightId = this.state.flightId;
       /*  this.state.allFlightIds.map((flightId) => {
            this.state.positionProperty[flightId] = this.getPositionProperty(flightId);   
        });
        this.setState(this.state);
        console.log(this.state.positionProperty);
        console.log(this.viewer);
        if (this.viewer) {
            this.viewer.zoomTo = this.viewer.entities;
        } */

    }


    getCesiumData(allFlightIds) {
        var cesiumData = null;
        var submissionData = {
            "allFlightIds" : allFlightIds
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

        let entity = (
            <Entity
                polyline={ new PolylineGraphics({
                    positions: positionsArr,
                    width: 3,
                    material: new PolylineOutlineMaterialProperty({
                        color: colorMap[type]["color"],
                        outlineColor: Color.BLACK,
                        outlineWidth: 2
                    }),
                    clampToGround: true
                }
               )}
            >
            </Entity>
        );

        return entity;
    }

    getLoadAGLEntity(type, data,flightId) {

        let detailsMap = {
            "default" : {
                positions : Cartesian3.fromDegreesArrayHeights(data),
                material: Color.WHITESMOKE.withAlpha(0),
            },
            "Taxiing" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.BLUE.withAlpha(0.8),
            },
            "Takeoff" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.BLUE.withAlpha(0.8),
            },
            "Climb" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.SADDLEBROWN.withAlpha(0.8),
            },
            "Cruise" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.LIGHTCYAN.withAlpha(0.8),
            }
        }
        let today = new Date();
        let entity = (
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

        let entity = (
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
        );

        return entity;
    }

    toggleCamera() {
        console.log(this.viewer);
        console.log(this.mainEntity);
        this.viewer.trackedEntity = null;

        console.log("Toggle Camera");
    }

    render() {

        var flightPhases = ["Select Flight Phase", "Show Taxiing", "Show Takeoff", "Show Climb", "Show Cruise to Final", "Show Full Flight"]
        var togglePathColors = ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "Black", "White"]
        var entities = [];
        var cesiumData = this.state.cesiumData;
        var clockStartTime = Cesium.JulianDate.fromIso8601("9999-12-31T00:00:00");
        var clockEndTime = Cesium.JulianDate.fromIso8601("0000-01-01T00:00:00")

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
            let geoFlightLoadAGLEntireDemoEntity = this.getLoadAGLEntity("default", infoAglDemo, flightId);
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
                            {
                                entities.map((entity) => {
                                    return entity;
                                })
                            }
                            </Viewer>
                    </div>
            </div>
                   /*  <div className="dropdown">
                        <select className="dropdown-select" id="dropdown">
                            {
                                flightPhases.map((phase, index) => (
                                    <option key={index}>{phase}</option>
                                ))
                            }
                                 
                        </select>
                    </div>
                    <div id="toggle-path-color">
                        <select className="cesium-button" id="color-options">
                            <option key={0}>Select Current Flight Path Color</option>
                            {
                                togglePathColors.map((color, index) => (<option key={index+1}>{color}</option>))
                            }
                        </select>
                    </div>
                    <div id="toggle-camera">
                        <button id="toggle-camera-btn" onClick={() => this.toggleCamera()}>Toggle Camera</button>
                    </div>
                    <div>
                        <button id="remove-entity"
                    </div> */
            // </div>
        );
    };
}

/* var cesiumPage = ReactDOM.render(
    <CesiumPage></CesiumPage>,
    document.querySelector("#cesium_page")
) 
 */
export default CesiumPage
