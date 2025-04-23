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

        this.state = {
            activeTag : null,
            editedTag : null,  //the tag currently being edited
            infoActive : false,
            adding : false,
            addFormActive : false,
            parent : props.parent
        };

        this.handleFormChange = this.handleFormChange.bind(this);
    }

    unToggleAddForm() {
        $("#show-add-form-button").removeClass('active');
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
        this.setToggle(-1);
        this.setState({
            infoActive : !this.state.infoActive,
            addFormActive : (this.state.addFormActive ? false : this.state.addFormActive)
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
     * Prepares to edit or just view a tag by creating a deep copy of the original tag
     * to be used later on to determine if any changes have been made.
     * @param tag the tag to edit
     */
    selectTag(index, tag) {
        this.unToggleAddForm();

        tag.index = index;
        this.setToggle(index);

        console.log("Editing tag: " + tag.hashId);
        if (this.state.activeTag == null || this.state.activeTag != tag) {
            this.state.activeTag = tag;
            this.state.editing = true;
            this.state.addFormActive = true;
        } else {
            this.clearActiveTag();
            this.state.editing = false;
            this.state.addFormActive = false;
        }
        this.state.adding = false;

        this.state.editedTag = cloneDeep(tag);

        this.setState(this.state);
    }

    clearActiveTag() {
        this.clearSelected();
        this.state.activeTag = null;
        this.setState(this.state);
    }

    clearSelected() {
        this.setToggle(-1);
    }
    
    setToggle(index) {
        if (this.props.flight.tags != null && this.props.flight.tags.length > 0) {
            let len = this.props.flight.tags.length;

            for (var i = 0; i < len; i++) {
                if (i != index) {
                    let id = '#tag_button_' + i;
                    $(id).removeClass('active');
                }
            }
        }
    }


    /**
     * shows @module errorModal when a tag has not been edited properly
     */
    showNoEditError() {
        errorModal.show("Error editing tag", "Please make a change to the tag first before pressing submit!");
    }

    removeTag() {

        if (this.state.activeTag==null)
            return;

        this.props.removeTag(this.props.flight.id, this.state.activeTag.hashId, false);
        this.setToggle(-1);
        this.setState({
            activeTag : null,
            editing : false,
            addFormActive : false
        });
    }

    deleteTag() {

        if (this.state.activeTag==null)
            return;

        this.setState({
            // activeTag : null,
            editing : false,
            addFormActive : false
        });

        this.props.deleteTag(this.props.flight.id, this.state.activeTag.hashId)
        .then((data) => {
            this.clearSelected();
        });

    }

    editTag() {

        if (this.state.activeTag==null)
            return;

        this.props.editTag(this.state.editedTag, this.state.activeTag);
        this.setState({
            addFormActive: false
        });

        let id = "#tag_img_" + this.state.activeTag.index;
        $(id).attr('data-title', 'Changes Saved!').tooltip('show');
        $("#tag_button_" + this.state.activeTag.index).removeClass('active');

        setTimeout(function() {
            $(id).tooltip('hide');
         }.bind(this), 5000)
    }

    createTag() {
       this.props.addTag(
            this.props.flight.id,
            $("#comName").val(), 
            $("#description").val(), 
            $("#color-picker-tag").val() 
        );

        this.setState({
            addFormActive: false
        });

        let id = "#tag_img_" + this.props.flight.tags.length - 1;
        console.log(id);

        setTimeout(function() {
            $(id).tooltip('hide');
         }.bind(this), 5000)
    }

    /**
     * Handles state changes for when the 'Create a new tag' option is selected
     */
    createClicked() {
        this.state = {
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
     * sets the state value editedTag which is used to make sure that the user has made an edit, before
     * enabling the submit button
     * @param e the onChange() event
     */
    handleFormChange(e) {
        if (e.target.id == 'comName') {
            this.state.editedTag.name = e.target.value;
        } else if (e.target.id == 'description') {
            this.state.editedTag.description = e.target.value;
        } else if (e.target.id == 'color-picker-tag') {
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
            minWidth:"2.50em",
            minHeight:"2.50em"
        };

        const styleColorInput = {
            height : "38",
        };

        let tags = this.props.flight.tags;
        let unassociatedTags = this.props.getUnassociatedTags(this.props.flight.id);

        let defName = "", defDescript = "", defColor=Colors.randomValue(), defAddAction = {}, tagStat = "";
        if (tags == null || tags.length == 0) {
            tagStat = (
                <div>
                    <div className="row m-1">
                        <div className="flex-basis m-1 p-3 card">
                            No tags have been added to this flight.
                        </div>
                    </div>
                </div>
            );
        } else {
           tagStat = ( 
                <div className={cellClasses} style={cellStyle}>
                    {
                        tags.map((tag, index) => {
                            return (
                                <button id={"tag_button_" + index} key={index} className={buttonClasses} data-bs-toggle="button" onClick={() => this.selectTag(index, tag)}>
                                    <i id={"tag_img_" + index} className="fa fa-tag m-1" data-bs-toggle="tooltip" data-bs-trigger='manual' data-bs-placement="right" style={{color : tag.color, marginRight : '10px'}}></i>
                                    {tag.name}
                                </button>
                            );
                        })
                    }
                    <button className={buttonClasses} style={styleButtonSq} title="Remove the selected tag from this flight" onClick={() => this.removeTag()}><i className="fa fa-minus" aria-hidden="true"></i></button>
                    <button
                        className={buttonClasses}
                        style={styleButtonSq}
                        title="Permanently delete the selected tag from all flights"
                        onClick={() => {
                            this.deleteTag();
                        }}
                    >
                        <i className="fa fa-trash" aria-hidden="true"/>
                    </button>
                    <button
                        className={buttonClasses}
                        style={styleButtonSq}
                        title="Clear all the tags from this flight"
                        onClick={() => {
                            this.props.clearTags(this.props.flight.id);
                            this.clearActiveTag();
                            this.setState({
                                // activeTag : null,
                                editing : false,
                                addFormActive : false
                            });
                        }}
                    >
                        <i className="fa fa-eraser" aria-hidden="true"/>
                    </button>
                </div> 
            );
        }

        let tagInfo = "";
        if (this.state.editing) {
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = () => this.editTag();
        }

        if (this.state.adding) {
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = () => this.createTag();
        }

        let submitButton = (
            <button id="submit-tag-button" className="btn btn-outline-secondary" style={styleButton} onClick={defAddAction} data-bs-toggle="tooltip" data-bs-trigger='manual' data-bs-placement="top" disabled>
                <i className="fa fa-check mr-1" aria-hidden="true"/>
                Submit
            </button> 
        );

        if (!this.state.editing || !this.tagEquals(activeTag, editedTag)) {
            submitButton = (
                <button id="submit-tag-button" className="btn btn-outline-secondary" data-bs-toggle="tooltip" data-bs-trigger='manual' data-bs-placement="top" style={styleButton} onClick={defAddAction} >
                    <i className="fa fa-check mr-1" aria-hidden="true"/>
                    Submit
                </button> 
            );
        }

        addDrop = (
            <div id="dropdown-item-button-add-tag" className="dropdown m-1">
                <button className="btn btn-outline-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Add a Tag
                </button>

                <div className="dropdown-menu" style={{maxHeight: "256px", overflowY: 'auto'}}>
                <button className="btn dropdown-item" onClick={() => this.createClicked()}>Create New Tag</button>
                {unassociatedTags != null && unassociatedTags.length > 0 &&
                    <div className="dropdown-divider"/>
                }
                {unassociatedTags != null && unassociatedTags.length > 0 &&
                    unassociatedTags.map((tag, index) => {
                        let style = {
                            backgroundColor : tag.color,
                            fontSize : "110%"
                        }
                        return (
                            <button key={index} className="btn dropdown-item" onClick={() => this.props.associateTag(tag.hashId, this.props.flight.id)}>
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
                            </button>
                        );
                    })
                }
                </div>
            </div>
        );

        if (this.state.addFormActive) {
            addForm = (
                <div className="d-flex flex-row m-1">
                    <div className="col-">
                        <div className="input-group">
                            <div className="input-group-prepend">
                                 <button type="button" className="btn input-group-text" title="Assign a color to this tag" onClick={(e) => $("#color-picker-tag").click()}>
                                     <i className="fa fa-tag" aria-hidden="true" style={{color: defColor}}></i>
                                 </button>
                                 <input key="cc-0" className="hidden" style={{display: "none"}} type="color" name="eventColor" value={defColor} onChange={(e) => this.handleFormChange(e)} id="color-picker-tag"/>
                            </div>
                            <input type="text" id="comName" className="form-control" onChange={this.handleFormChange} value={defName} placeholder="Common Name"/>
                        </div>
                    </div>
                    <div className="col-sm">
                        <div className="input-group">
                            <div className="input-group-prepend">
                                <span className="input-group-text">
                                    <span className="fa fa-info"></span>
                                </span>
                            </div>
                          <input type="text" id="description" className="form-control" onChange={this.handleFormChange} value={defDescript} placeholder="Description"/>
                        </div>
                    </div>
                    <div className="col-sm ml-0">
                        <div className="input-group">
                            {submitButton}
                        </div>
                    </div>
                </div>
            );
        }


        return (
            <div className="w-100">

                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"} style={{marginBottom:"0"}}>
                    <div className="d-flex flex-column mr-3" style={{width: "16px", minWidth:"16px", maxWidth:"16px", height: "16px"}}>
                        <i className='fa fa-plus ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Tags
                    </div>
                </b>

                {tagStat} 
                <div className="flex-row m-1 mb-2">
                    {addDrop}{addForm}
                </div>
            </div>
        );
    }
}


export { Tags };