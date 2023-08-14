import ReactDOM from "react-dom";
import React from "react";
import {
    Cartesian3,
    Ion, Math, IonResource,
    ModelGraphics, JulianDate,
    SampledPositionProperty,
    TimeIntervalCollection,
    TimeInterval,
    VelocityOrientationProperty,
    PathGraphics, Color,
    PolylineOutlineMaterialProperty, WallGraphics, PolylineGraphics, CornerType
} from "cesium";
import {Viewer, Entity, Scene, Globe, Clock} from "resium";


class CesiumPage extends React.Component {
    constructor(props) {
        super(props);
        const urlParams = new URLSearchParams(window.location.search);
        let flightId = urlParams.get("flight_id");
        this.state = {
            flightId : flightId,
            airFrameModelsURI : {
                "airplaneURI" : 1084423,
                "droneURI" : 1117220,
                "scanEagleURI" : 1196119
            },
            modelGraphicsInfo : {
                minimumPixelSize: 128,
                maximumScale: 20000,
                scale: 1,
            }
        }
    }

    getCesiumData() {
        var cesiumData = null;
        var submissionData = {
            "flight_id" : this.state.flightId
        };
        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                cesiumData =  response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
            },
            async: false
        });
        return cesiumData;

    }

    loadAirFrameModels() {

        // const airplaneURI = Cesium.IonResource.fromAssetId(1084423);
    }
    render() {
        // console.log(cesium_data);

        var cesiumData = this.getCesiumData();
        console.log(cesiumData);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3OTU3MDVjYS04ZGRiLTRkZmYtOWE5ZC1lNTEzMTZlNjE5NWEiLCJpZCI6OTYyNjMsImlhdCI6MTY1NDMxNTk2N30.c_n2k_FWWisRoXnAFVGs6Nbxk0NVFmrIpqL12kjE7sA";
        Math.setRandomNumberSeed(9);

        var flightPhases = ["Select Flight Phase", "Show Taxiing", "Show Takeoff", "Show Climb", "Show Cruise to Final", "Show Full Flight"]
        var togglePathColors = ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "Black", "White"]

        var positions = cesiumData[this.state.flightId].flightGeoInfoAgl;
        var startTime = cesiumData[this.state.flightId].startTime;
        var endTime = cesiumData[this.state.flightId].endTime;

        var model = new ModelGraphics({
            minimumPixelSize : 128,
            maximumScale : 20000,
            scale : 1
        })
        if (cesiumData[this.state.flightId].airframeType == "Fixed Wing" || cesiumData[this.state.flightId].airframeType == "UAS Fixed Wing") {
            model.uri = IonResource.fromAssetId(this.state.airFrameModelsURI["airplaneURI"])
        }
        else if (cesiumData[this.state.flightId].airframeType == "UAS Rotorcraft") {
            model.uri = IonResource.fromAssetId(this.state.airFrameModelsURI["droneURI"])
        }
        var positionProperty = new SampledPositionProperty();
        var infoAglDemo = cesiumData[this.state.flightId].flightGeoInfoAgl;
        console.log(positions);
        var pathColor = Color.fromRandom();
        return (
            <div>
                <div id="cesiumContainer">
                    <Viewer full
                        timeline={true}
                        scene3DOnly={true}
                        selectionIndicator={true}
                        baseLayerPicker={false}
                    >
                        <Clock
                            startTime={JulianDate.fromIso8601(startTime)}
                            stopTime={JulianDate.fromIso8601(endTime)}
                            currentTime={JulianDate.fromIso8601(startTime)}
                            shouldAnimate={true}
                            multiplier={25}

                        >
                        </Clock>
                        <Scene>
                        </Scene>
                        <Globe
                                depthTestAgainstTerrain = {true}>
                        </Globe>
                        <Entity
                                position={positionProperty}
                                model={model}
                                orientation={positionProperty}
                                point={{ pixelSize: 10 , color: Color.RED}}
                                availability={new TimeIntervalCollection([new TimeInterval({start: startTime, stop: endTime})])}
                                path={new PathGraphics({
                                    width : 5,
                                    material : new PolylineOutlineMaterialProperty({
                                        color : pathColor,
                                        outlineColor: pathColor,
                                        outlineWidth: 5
                                    })
                                    }
                                )}
                        >
                        </Entity>
                        <Entity
                            name={"NGAFID CESIUM DEMO: " + this.state.flightId}
                            wall={ new WallGraphics({
                                positions : Cartesian3.fromDegreesArrayHeights(infoAglDemo),
                                material: Color.WHITESMOKE.withAlpha(0),
                                cornerType: CornerType.BEVELED,
                            })}
                        >
                        </Entity>
                        <Entity
                            polyline={ new PolylineGraphics({
                                positions : Cartesian3.fromDegreesArrayHeights(infoAglDemo),
                                width: 1,
                                material: new PolylineOutlineMaterialProperty({
                                    color: Color.LEMONCHIFFON,
                                    outlineWidth: 2,
                                    outlineColor: Color.BLACK,
                                }),
                                clampToGround: true,
                            })}
                        >
                        </Entity>
                    </Viewer>
                </div>
                    <div id="toolbar">
                        <select className="cesium-button" id="dropdown">
                            {
                                flightPhases.map((phase, index) => (<option key={index}>{phase}</option>))
                            }
                        </select>
                </div>
                <div id="toggle-path-color">
                    <select className="cesium-button" id="color-options">
                        <option value="0">Select Current Flight Path Color</option>
                        {
                            togglePathColors.map((color, index) => (<option key={index}>{color}</option>))
                        }
                    </select>
                </div>
            </div>
        )

    }
}

var cesiumPage = ReactDOM.render(
    <CesiumPage></CesiumPage>,
    document.querySelector("#cesium_page")
)

// export default CesiumPage