<html>
<head>
    <title>Map Test</title>


    <link href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css" rel="stylesheet" />
    <link href="https://openlayers.org/en/v4.6.4/css/ol.css" rel="stylesheet">
    <!-- The line below is only needed for old environments like Internet Explorer and Android 4.x -->
    <script src="https://cdn.polyfill.io/v2/polyfill.min.js?features=requestAnimationFrame,Element.prototype.classList,URL"></script>

    <style>
        .ui-datepicker-calendar {
            display: none;
        }

        .vertical-line {
            border-left: 1px solid hsl(214, 7%, 80%);
        }

        #map-container {
            margin-top: 2em;
        }

        .selected-source {
            color: #ffffff;
            text-shadow: 0 1px 2px rgba(0, 0, 0, 0.20);
            background-image: linear-gradient(-180deg, #80d1f3 0%, #4a90e2 100%);
            box-shadow: 0 1px 2px 0 rgba(74, 144, 226, 0.44), 0 2px 8px 0 rgba(0, 0, 0, 0.14);
        }

        .ol-popup {
            position: absolute;
            background-color: white;
            -webkit-filter: drop-shadow(0 1px 4px rgba(0,0,0,0.2));
            filter: drop-shadow(0 1px 4px rgba(0,0,0,0.2));
            padding: 15px;
            border-radius: 10px;
            border: 1px solid #cccccc;
            bottom: 12px;
            left: -50px;
            min-width: 155px;
        }
        .ol-popup:after, .ol-popup:before {
            top: 100%;
            border: solid transparent;
            content: " ";
            height: 0;
            width: 0;
            position: absolute;
            pointer-events: none;
        }
        .ol-popup:after {
            border-top-color: white;
            border-width: 10px;
            left: 48px;
            margin-left: -10px;
        }
        .ol-popup:before {
            border-top-color: #cccccc;
            border-width: 11px;
            left: 48px;
            margin-left: -11px;
        }
        .ol-popup-closer {
            text-decoration: none;
            position: absolute;
            top: 2px;
            right: 8px;
        }
        .ol-popup-closer:after {
            content: "✖";
        }
    </style>
</head>


<body>
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-10 col-md-offset-1">
                <div id="map-container" class="col-md-12">
                    <div id="set_source_btns" class="btn-group btn-group-justified" role="group"></div>

                    <div id="map" class="map"></div>
                    <div id="popup" class="ol-popup">
                        <a href="#" id="popup-closer" class="ol-popup-closer"></a>
                        <div id="popup-content"></div>
                    </div>

                    <div class="btn-group btn-group-justified" role="group">
                        <a href="" id="export" class="btn btn-default">
                            <span class="glyphicon glyphicon-download-alt"></span>
                            Download PNG
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- <script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script> -->
    <script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>

    <script src="https://openlayers.org/en/v4.6.4/build/ol.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Turf.js/5.1.5/turf.min.js" integrity="sha256-V9GWip6STrPGZ47Fl52caWO8LhKidMGdFvZbjFUlRFs=" crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/FileSaver.js/1.3.3/FileSaver.min.js" integrity="sha256-FPJJt8nA+xL4RU6/gsriA8p8xAeLGatoyTjldvQKGdE=" crossorigin="anonymous"></script>

    <!--
    <script src="{{ elixir('js/airport-runway-autocomplete.js') }}"></script>
    <script src="{{ elixir('js/datepicker-utils.js') }}"></script>
-->

    <script type="text/javascript">
        function transformPoint(point) {
            point = point instanceof Array ? point : point.geometry.coordinates;
            return ol.proj.fromLonLat(point);
        }

        function transformPoints(points) {
            return points.map(function (point) { return transformPoint(point); });
        }

        $(function () {
            var allData = [];
            var $setSourceBtnsContainer = $('div#set_source_btns');
            var vectorSources = [];

            function createSetSourceBtn(idx) {
                return $('<div>', {class: 'btn-group', role: 'group'}).append(
                    $('<button>', {id: 'set_source_' + idx, class: 'btn btn-default', text: 'Approach #' + (idx + 1)})
                );
            }

            function createVectorLayer(source) {
                return new ol.layer.Vector({
                    source: source,
                    style: styleFunction
                });
            }

            function createVectorSource(approach) {
                var phases = approach.phases;
                var features = [];
                Object.keys(phases).forEach(function (key) {
                    var coordinates = transformPoints(phases[key].coordinates);

                    if (coordinates.length > 1) {
                        features.push(turf.lineString(
                            coordinates, {
                                id: key,
                                flightId: approach.flight_id,
                                approachId: approach.approach_id,
                                type: phases[key].type,
                                severity: phases[key].severity
                            }
                        ));
                    }
                });

                var touchdown = [
                    approach.runway.touchdown_lon,
                    approach.runway.touchdown_lat
                ];
                var runwayBearing = approach.runway.true_course;

                // We need the reciprocal of the runway heading for calculating
                // the extended center line point
                var reverseBearing = (180 + runwayBearing) % 360;

                // Need to normalize bearing from -180 to 180 degrees from north
                if (reverseBearing > 180)
                    reverseBearing -= 360;

                // Calculate a point that is 1 mile in the opposite heading as the
                // runway for an extended center line
                var touchdownPoint = turf.point(touchdown);
                var extendedTouchdownPoint = turf.destination(
                    touchdownPoint, 1, reverseBearing, {units: 'miles'}
                );
                var extendedCenterLineString = turf.lineString(
                    transformPoints([extendedTouchdownPoint, touchdownPoint]),
                    {id: 'extended_center_line'}
                );

                // Combine extended center line & flight path into a single feature
                // collection
                var collection = turf.featureCollection([
                    ...features, extendedCenterLineString
                ]);

                return new ol.source.Vector({
                    features: (new ol.format.GeoJSON()).readFeatures(collection)
                });
            }

            function setVectorSource(idx) {
                vectorLayer.setSource(vectorSources[idx]);

                var runway = allData[idx].runway;
                transitionMapViewTo(runway);
            }

            function transitionMapViewTo(runway) {
                var touchdownPoint = turf.point([
                    runway.touchdown_lon,
                    runway.touchdown_lat
                ]);

                flyTo(
                    transformPoint(touchdownPoint),
                    turf.degreesToRadians(360 - runway.true_course),
                    3000,  // milliseconds
                    function () {}
                );
            }

            function flyTo(location, rotation, duration, done) {
                var zoom = mapView.getZoom();
                var parts = 2;
                var called = false;
                var callback = function (complete) {
                    --parts;
                    if (called) {
                        return;
                    }
                    if (parts === 0 || !complete) {
                        called = true;
                        done(complete);
                    }
                };
                mapView.animate({
                    center: location,
                    rotation: rotation,
                    duration: duration
                }, callback);
                mapView.animate({
                    zoom: zoom - 3,
                    duration: duration / 2
                }, {
                    zoom: 15,
                    duration: duration / 2
                }, callback);
            }

            var styles = {
                'stop-and-go': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#529699',
                        width: 2
                    })
                }),
                'touch-and-go': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#004144',
                        width: 2
                    })
                }),
                'go-around': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#005906',
                        width: 2
                    })
                }),
                0: new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#6ac870',
                        width: 2
                    })
                }),
                1: new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#e7eb2e',
                        width: 2
                    })
                }),
                2: new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#be2f2b',
                        width: 2
                    })
                }),
                'landing': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#fe8b88',
                        width: 2
                    })
                }),
                'takeoff': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#710300',
                        width: 2
                    })
                }),
                'extended_center_line': new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#000000',
                        width: 1
                    })
                })
            };

            var styleFunction = function (feature) {
                var props = feature.getProperties();

                return styles[props.severity || props.type || props.id];
            };

            var popup_container = document.getElementById('popup'),
                popup_content = document.getElementById('popup-content'),
                popup_closer = document.getElementById('popup-closer');

            /**
             * Create an overlay to anchor the popup to the map.
             */
            var overlay = new ol.Overlay({
                element: popup_container,
                autoPan: true,
                autoPanAnimation: {
                    duration: 250
                }
            });

            /**
             * Add a click handler to hide the popup.
             * @return {boolean} Don't follow the href.
             */
            popup_closer.onclick  = function () {
                overlay.setPosition(undefined);
                popup_closer.blur();
                return false;
            };

            var vectorLayer = new ol.layer.Vector({
                style: styleFunction
            });

            var mapView = new ol.View({
                // Setting the below as defaults before the data is dynamically
                // loaded
                center: [-97.0329, 47.9253],
                zoom: 5
            });

            var osmSourceLayer = new ol.layer.Tile({
                preload: 4,
                source: new ol.source.OSM()
            });

            var map = new ol.Map({
                layers: [
                    osmSourceLayer,
                    vectorLayer
                ],
                overlays: [overlay],
                target: document.getElementById('map'),
                loadTilesWhileAnimating: true,
                view: mapView
            });

            // display popup on click
            map.on('singleclick', function (evt) {
                var feature = map.forEachFeatureAtPixel(evt.pixel, function (feature) {
                    return feature;
                });

                if (feature && feature.get('id') !== 'extended_center_line') {
                    //var msg = 'Flight ID: <a href="{{ url('approach/flights?') }}' +
                        //$.param({'flight_id[]': feature.get('flightId')}) + '">' + feature.get('flightId') + '</a><br>' +
                        //'Approach ID: ' + feature.get('approachId');
                    popup_content.innerHTML = msg;
                    overlay.setPosition(evt.coordinate);
                }
            });

            // change mouse cursor when over marker
            map.on('pointermove', function (evt) {
                if (evt.dragging) {
                    overlay.setPosition(undefined);
                    return;
                }

                var pixel = map.getEventPixel(evt.originalEvent);
                var hit = map.hasFeatureAtPixel(pixel);
                map.getTarget().style.cursor = hit ? 'pointer' : '';
            });

            $setSourceBtnsContainer.on('click', 'button[id^="set_source_"]', function () {
                var $sourceBtns = $setSourceBtnsContainer.find('button[id^="set_source_"]');
                var idx = $sourceBtns.index(this);
                $sourceBtns.removeClass('selected-source');
                $(this).addClass('selected-source');
                setVectorSource(idx);
            });

            var mapTransitionOccurred = false;

            });
    </script>
</body>
