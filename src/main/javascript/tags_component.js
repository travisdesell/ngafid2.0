import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import { Colors } from "./map.js";

import { errorModal } from "./error_modal.js";
import { confirmModal } from "./confirm_modal.js";

const cloneDeep = require('clone-deep');

//This will be helpful for text inputs
function invalidString(str){
    return (str == null || str.length < 0 || /^\s*$/.test(str));
}



/**
 * Tags module
 * Houses the state and information for user-defined tags
 * @module flights/Tags
 */
class Tags extends React.Component {

    /**
     * ReactJS Constructor
     * Intializes the state of this component
     * @param props the props that are sent from the parent component
     */
    constructor(props) {
        super(props);

        let pTags = [];
        if (props.tags != null) {
            pTags = props.tags;
        }

        this.state = {
            tags : pTags,
            unassociatedTags : [],
            flightIndex : props.flightIndex,
            flightId : props.flightId,
            activeTag : null,
            editedTag : null,  //the tag currently being edited
            infoActive : false,
            addActive : false,
            editing : false,
            adding : false,
            addFormActive : false,
            assocTagActice : false,
            parent : props.parent
        };
        this.handleFormChange = this.handleFormChange.bind(this);
    }

    /**
     * called everytime props are updated
     * @param oldProps the old props before the update
     */
    //componentDidUpdate(oldProps) {
        //console.log("props updated");
        //const newProps = this.props;
          //if (oldProps.tags !== newProps.tags) {
            //this.state.tags = this.props.tags;
            //this.state.addFormActive = false; //close the add form to indicate the tag has been edited or no longer exists
            //this.setState(this.state);
          //}
    //}

    /**
     * Handles the event for which the add button is pressed
     */
    addClicked() {
        this.state.addActive = !this.state.addActive;
        this.state.infoActive = !this.state.infoActive;

        if (this.state.addFormActive) {
            this.state.addFormActive = false;
        }

        this.setState({
            unassociatedTags : this.props.getUnassociatedTags(this.state.flightId)
        });
    }

    /**
     * Used to compare two tags for equality
     * @param tagA the first tag to compare
     * @param tagB the second tag to compare
     * @return a boolean representing whether or not the two tags are equal
     */
    tagEquals(tagA, tagB) {
        return (tagA != null && tagB != null) && //they cant be null!
            tagA.name == tagB.name &&
            tagA.description == tagB.description &&
            tagA.color == tagB.color;
    }

    /**
     * Prepares to edit a tag by creating a deep copy of the original tag
     * to be used later on to determine if any changes have been made.
     * @param tag the tag to edit
     */
    editTag(tag) {
        console.log("Editing tag: " + tag.hashId);
        if (this.state.activeTag == null || this.state.activeTag != tag) {
            this.state.editing = true;
            this.state.addFormActive = true;
        } else {
            this.state.editing = !this.state.editing;
            this.state.addFormActive = !this.state.addFormActive;
        }
        this.state.adding = false;

        this.state.activeTag = tag;
        this.state.editedTag = cloneDeep(tag);

        this.setState(this.state);
    }

    /**
     * shows @module errorModal when a tag has not been edited properly
     */
    showNoEditError() {
        errorModal.show("Error editing tag", "Please make a change to the tag first before pressing submit!");
    }

    /**
     * invoked by another function when the user has confirmed they would like to delete the tag permanently
     */
    confirmDelete() {
        this.removeTag(this.state.activeTag.hashId, true);
    }

    /**
     * Handles state changes for when the 'Create a new tag' option is selected
     */
    createClicked() {
        this.state = {
            addActive : true,
            adding : true,
            editing : false,
        };
        this.showAddForm();
    }

    /**
     * Shows the form for adding and/or editing a tag
     */
    showAddForm() {
        this.state.addFormActive = !this.state.addFormActive;
        this.state.editedTag = {
            name : "",
            description : "",
            color : Colors.randomValue()
        };
        this.setState(this.state);
    }

    /**
     * Updates the parent when the flights on the current page have changed
     */
    //updateFlights(flights) {
        //console.log("sending new flights to parents");
        //console.log(flights);
        //this.state.parent.updateFlights(flights);
    //}

    /**
     * Updates the parent of changes ONLY made to this flights tags
     */
    //updateParent(tags) {
        //this.state.parent.invokeUpdate(tags);
    //}

    /**
     * sets the state value editedTag which is used to make sure that the user has made an edit, before
     * enabling the submit button
     * @param e the onChange() event
     */
    handleFormChange(e) {
        if (e.target.id == 'comName') {
            this.state.editedTag.name = e.target.value;
        }
        else if (e.target.id == 'description') {
            this.state.editedTag.description = e.target.value;
        }
        else if (e.target.id == 'color') {
            this.state.editedTag.color = e.target.value;
        }
        this.setState(this.state);
    }

    /**
     * Renders the Tags component
     */
    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let vcellStyle = { "overflowY" : "visible"};
        let addForm = "";
        let addDrop = "";
        let activeTag = this.state.activeTag;
        let editedTag = this.state.editedTag;
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 10 10em",
        };
        const styleButtonSq = {
            flex : "0 2 2em",
        };

        const styleColorInput = {
            height : "38",
        };

        let tags = this.props.flight.tags;
        let unassociatedTags = this.state.unassociatedTags;
        let hasOtherTags = (unassociatedTags != null);

        let activeId = -1;
        if (this.state.activeTag != null) {
            activeId = activeTag.hashId;
        }

        let defName = "", defDescript = "", defColor=Colors.randomValue(), defAddAction = {}, tagStat = "";
        if (tags == null || tags.length == 0) {
            tagStat = (<div><b className={"p-2"} style={{marginBottom:"2"}}>No tags yet!</b>
                <button className={buttonClasses} style={styleButtonSq} data-toggle="button" title="Add a tag to this flight" onClick={() => this.addClicked()}>Add a tag</button>
            </div>);
        } else {
           tagStat = ( 
                <div className={cellClasses} style={cellStyle}>
                {
                    tags.map((tag, index) => {
                        var cStyle = {
                            flex : "0 10 10em",
                            color : tag.color, 
                            fontWeight : '650'
                        };
                        return (
                                <button key={index} className={buttonClasses} onClick={() => this.editTag(tag)}>
                                    <i className="fa fa-tag p-1" style={{color : tag.color, marginRight : '10px'}}></i>
                                    {tag.name}
                                </button>
                        );
                    })
                }
                <button className={buttonClasses} style={styleButtonSq} aria-pressed={this.state.addActive} title="Add a tag to this flight" onClick={() => this.addClicked()}><i className="fa fa-plus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Remove the selected tag from this flight" onClick={() => this.removeTag(activeId, false)}><i className="fa fa-minus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Permanently delete the selected tag from all flights" onClick={() => this.deleteTag()}><i className="fa fa-trash" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Clear all the tags from this flight" onClick={() => this.clearTags()}><i className="fa fa-eraser" aria-hidden="true"></i></button>
                </div> );
        }

        let tagInfo = "";
        if (this.state.editing) {
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = () => {this.props.editTag(this.state.editedTag, this.state.activeTag)} ;
        }

        if (this.state.adding) {
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = () => this.props.addTag(
                this.state.flightId,
                $("#comName").val(), 
                $("#description").val(), 
                $("#color").val() 
            );
        }

        let submitButton = (
                        <button className="btn btn-outline-secondary" style={styleButtonSq} onClick={defAddAction} disabled>
                            <i className="fa fa-check" aria-hidden="true"></i>
                                Submit
                        </button> );
        if (!this.state.editing || !this.tagEquals(activeTag, editedTag)) {
            submitButton = (
                        <button className="btn btn-outline-secondary" style={styleButtonSq} onClick={defAddAction} >
                            <i className="fa fa-check" aria-hidden="true"></i>
                                Submit
                        </button> );
        }


        if (this.state.addActive) {
            addDrop =
                <DropdownButton className={cellClasses + {maxHeight: "256px", overflowY: 'scroll'}} id="dropdown-item-button" variant="outline-secondary" title="Add a tag to this flight">
                    <Dropdown.Item as="button" onSelect={() => this.createClicked()}>Create a new tag</Dropdown.Item>
                    {unassociatedTags != null &&
                        <Dropdown.Divider />
                    }
                    {unassociatedTags != null &&
                        unassociatedTags.map((tag, index) => {
                            let style = {
                                backgroundColor : tag.color,
                                fontSize : "110%"
                            }
                            return (
                                    <Dropdown.Item key={index} as="button" onSelect={() => this.props.associateTag(tag.hashId, this.state.flightId)}>
                                        <div className="row">
                                            <div className="col-xs-1 text-center">
                                                <span className="badge badge-pill badge-primary" style={style}>
                                                    <i className="fa fa-tag" aria-hidden="true"></i>
                                                </span>
                                            </div>
                                            <div className="col text-center">
                                                {tag.name}
                                            </div>
                                        </div>
                                    </Dropdown.Item>
                            );
                        })
                    }
                    </DropdownButton>
        }
        if (this.state.addFormActive) {
            addForm =
            <div className="row p-4">
                <div className="col-">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <span className="input-group-text">
                                <span className="fa fa-tag"></span>
                            </span>
                        </div>
                        <input type="text" id="comName" className="form-control" onChange={this.handleFormChange} value={defName} placeholder="Common Name"/>
                    </div>
                </div>
                <div className="col-sm">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <span className="input-group-text">
                                <span className="fa fa-list"></span>
                            </span>
                        </div>
                      <input type="text" id="description" className="form-control" onChange={this.handleFormChange} value={defDescript} placeholder="Description"/>
                    </div>
                </div>
                <div className="col-">
                    <div style={{flex: "0 0"}}>
                      <input type="color" name="eventColor" value={defColor} onChange={this.handleFormChange} id="color" style={styleColorInput}/>
                    </div>
                </div>
                <div className="col-sm">
                    <div className="input-group">
                        {submitButton}
                    </div>
                </div>
            </div>
        }


        return (
            <div>
                <div>
                    <b className={"p-1"} style={{styleButton}}>Associated Tags:</b>
                </div>
                {tagStat} 
                <div className="flex-row p-1">
                    {addDrop}{addForm}
                </div>
            </div>
        );
    }
}


export { Tags };
