import 'bootstrap';
import React from "react";

import {fromLonLat} from 'ol/proj';
import { map } from "./map";

class Itinerary extends React.Component {

    constructor(props) {

        super(props);

        this.state = {
            color : this.props.color
        };

    }

    itineraryClicked(index) {
        
        this.props.showMap();

        const stop = this.props.itinerary[index];
        const modifiedIndex = parseInt(stop.minAltitudeIndex) - this.props.nanOffset;
        //  console.log("index: " + stop.minAltitudeIndex + ", nanOffset: " + this.props.nanOffset + ", modifeid_index: " + modifiedIndex);

        const latlon = this.props.coordinates[modifiedIndex];
        //  console.log(latlon);

        const coords = fromLonLat(latlon);
        const MAP_ZOOM_LEVEL_DEFAULT = 13;
        map.getView().animate({
            center: coords,
            zoom: MAP_ZOOM_LEVEL_DEFAULT
        });

    }

    changeColor(event) {

        this.setState({
            color : event.target.value
        });
        
    }

    selectLayer(selectedLayer) {

        console.log("Setting selected layer: ", selectedLayer);
        for (let i = 0; i < this.props.layers.length; i++) {

            const layer = this.props.layers[i];
            layer.setVisible(layer.get('name').includes(selectedLayer));
            console.log("Setting layer: ", layer);

        }

        this.setState(this.state);

    }

    setDefaultLayer() {

        const defaultLayerName = 'Itinerary'; //<-- changeme if we want the default layer to be something different

        this.selectLayer(defaultLayerName);
    }

    getSelectedLayer() {

        for (let i = 0; i < this.props.layers.length; i++) {

            const layer = this.props.layers[i];
            console.log("Got selected layer: ", layer);

            //Layer is visible and not a map layer, return the description
            if (layer.get('visible') && !layer.get('nMap'))
                return layer.get('description');
            
        }

        this.setDefaultLayer();

        const LAYER_DESCRIPTION_DEFAULT = 'Itinerary with Phases';
        return LAYER_DESCRIPTION_DEFAULT;

    }

    render() {

        console.log("Rendering itinerary component...");

        const cellClasses = "d-flex flex-row p-1 mx-1";
        const cellStyle = { "overflowX" : "auto" };
        const buttonClasses = "m-1 btn expand-import-button btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        return (
            <div className="w-100">

                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"} style={{marginBottom:"0"}}>
                    <div className="d-flex flex-column mr-3" style={{width: "16px", minWidth:"16px", maxWidth:"16px", height: "16px"}}>
                        <i className='fa fa-map ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Itinerary
                    </div>
                </b>

                <div className={cellClasses} style={cellStyle}>
                    <div style={{flex: "0 0"}}>
                        <input type="color" name="itineraryColor" value={this.state.color} onChange={(event) => {this.changeColor(event); this.props.flightColorChange(this.props.parent, event);}} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                    </div>

                    <button className="m-1 btn btn-outline-dark dropdown-toggle" style={styleButton} type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <i className="fa fa-map-o p-1"></i>
                        {this.getSelectedLayer()}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    {
                        this.props.layers.map((layer, index) => {
                            if (!layer.get('nMap')) {
                                return (
                                    <button className="dropdown-item" type="button" key={index} onClick={() => this.selectLayer(layer.get('name'))} disabled={layer.get('disabled')}>{layer.get('description')}</button>
                                );
                            }
                        })
                    }
                    </div>

                    {
                        this.props.itinerary.map((stop, index) => {

                            let identifier = stop.airport;

                            //Got runway, add it to the identifier
                            if (stop.runway != null)
                                identifier += ` (${  stop.runway  })`;

                            return (
                                <button className={buttonClasses} key={index} style={styleButton} onClick={() => this.itineraryClicked(index)}>
                                    { identifier }
                                </button>
                            );
                        })
                    }
                </div>
            </div>
        );

    }
}

export { Itinerary };
