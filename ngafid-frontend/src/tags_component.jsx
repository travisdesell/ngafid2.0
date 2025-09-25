import 'bootstrap';
import React from "react";

import { Colors } from "./map";

import { showErrorModal } from "./error_modal";

import cloneDeep from 'clone-deep';


import './index.css'; //<-- include Tailwind


export const TAG_ID_NONE = -1;
export const TAG_ID_ALL  = -2;  //<-- For when we refer to all of a flight's tags (e.g. when clearing)


/**
 * Tags module
 * Houses the state and information for user-defined tags
 * @module flights/Tags``
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
            activeTagID : null,
            editedTag : null,  //the tag currently being edited
            infoActive : false,
            adding : false,
            addFormActive : false,
            parent : props.parent
        };

        this.handleFormChange = this.handleFormChange.bind(this);

        this._addTagButton = null;
        this._addTagDropdown = null;

    }

    componentDidUpdate(prevProps) {

        const prevIds = (prevProps.flight.tags || []).map(t => t.hashId).join(',');
        const currIds = (this.props.flight.tags || []).map(t => t.hashId).join(',');

        //No changes to tags, exit
        if (prevIds === currIds)
            return;

        const { activeTagID } = this.state;

        //Active Tag ID not found, exit
        if (!activeTagID)
            return;

        const tagStillExists = (this.props.flight.tags || []).some(t => t.hashId === activeTagID);

        //Tag exists, clear selection
        if (!tagStillExists)
            this.setState({
                activeTagID: null,
                editing: false,
                addFormActive: false
            });

    }

    uniqueTagsById = (list) => {

        const map = new Map();
        (list || []).forEach(t => {
            
            if (!map.has(t.hashId))
                map.set(t.hashId, t);

        });

        return Array.from(map.values());

    };

    getActiveTagFromProps() {
        const { activeTagID } = this.state;
        const tags = this.uniqueTagsById(this.props.flight.tags);
        return tags.find(t => t.hashId === activeTagID) || null;
    }

    unToggleAddForm() {
        $("#show-add-form-button").removeClass('active');
    }


    /**
     * Handles the event for which the add button is pressed
     */
    addClicked() {
        this.setState(prevState => ({
            infoActive: !prevState.infoActive,
            addFormActive: prevState.addFormActive ? false : prevState.addFormActive
        }));
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
    selectTag(tag) {

        this.unToggleAddForm();

        const nextActiveID = (this.state.activeTagID === tag.hashId) ? null : tag.hashId;

        if (nextActiveID) {

            console.log("Selecting Tag: ", tag.hashId, "Next Active ID: ", nextActiveID, "Name: ", tag.name);

            this.setState({
                activeTagID: nextActiveID,
                editing: true,
                addFormActive: true,
                adding: false,
                editedTag: cloneDeep(tag)
            });

        } else {

            console.log("Clearing Selected Tag: ", tag.hashId, "Next Active ID: ", nextActiveID, "Name: ", tag.name);

            this.setState({
                activeTagID: null,
                editing: false,
                addFormActive: false,
                adding: false,
                editedTag: null,
            });

        }

    }

    clearActiveTag() {
        this.setState({ activeTagID: null });
    }


    /**
     * shows @module errorModal when a tag has not been edited properly
     */
    showNoEditError() {
        showErrorModal("Error editing tag", "Please make a change to the tag first before pressing submit!");
    }

    removeTag() {

        const { activeTagID } = this.state;
        if (activeTagID == null)
            return;

        const tagResponse = this.props.removeTag(this.props.flight.id, activeTagID, false);
        tagResponse.then(() => {
            this.setState({
                activeTagID : null,
                editing : false,
                addFormActive : false
            });

        });

    }

    deleteTag() {

        const activeTag = this.getActiveTagFromProps();
        if (!activeTag)
            return;

        this.setState({
            editing : false,
            addFormActive : false
        });

        this.props.deleteTag(this.props.flight.id, this.state.activeTagID)
            .then(() => {
                this.clearActiveTag();
            });

    }

    editTag() {

        const activeTag = this.getActiveTagFromProps();
        if (!activeTag)
            return;

        this.props.editTag(this.state.editedTag, activeTag);
        this.setState({
            addFormActive: false
        });

        const imgSel = `#tag_img_${this.props.flight.id}_${activeTag.hashId}`;
        $(imgSel).attr('data-title', 'Changes Saved!').tooltip('show');
        setTimeout(() => $(imgSel).tooltip('hide'), 5000);

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

        const id = `#tag_img_${this.props.flight.id}_${this.props.flight.tags.length - 1}`;
        console.log(id);

        setTimeout(function() {
            $(id).tooltip('hide');
         }.bind(this), 5000);
    }

    /**
     * Handles state changes for when the 'Create a new tag' option is selected
     */
    createClicked() {
        this.setState({
            adding : true,
            editing : false,
        });
        this.showAddForm();
    }

    /**
     * Shows the form for adding and/or editing a tag
     */
    showAddForm() {
        this.setState((prevState) => ({
            addFormActive: !prevState.addFormActive,
            editedTag: {
                name: "",
                description: "",
                color: Colors.randomValue()
            }
        }));
    }

    /**
     * sets the state value editedTag which is used to make sure that the user has made an edit, before
     * enabling the submit button
     * @param e the onChange() event
     */
    handleFormChange(e) {
        const editedTag = { ...this.state.editedTag };
        if (e.target.id == 'comName') {
            editedTag.name = e.target.value;
        } else if (e.target.id == 'description') {
            editedTag.description = e.target.value;
        } else if (e.target.id == 'color-picker-tag') {
            editedTag.color = e.target.value;
        }
        this.setState({ editedTag });
    }

    /**
     * Renders the Tags component
     */
    render() {

        const cellClasses = "d-flex flex-row p-1";
        const cellStyle = { "overflowX" : "auto" };
        let addForm = "";
        const activeTag = this.getActiveTagFromProps();
        const buttonClasses = "m-1 btn btn-outline-secondary flex! flex-row! gap-2! items-center! justify-between!";
        const styleButton = {
            flex : "0 10 10em",
        };

        const tags = this.uniqueTagsById(this.props.flight.tags);
        const hasAnyTags = (tags != null && tags.length > 0);
        const unassociatedTags = this.props.getUnassociatedTags(this.props.flight.id);
        const noTagSelected = (!activeTag);

        let defName = "", defDescript = "", defColor=Colors.randomValue(), defAddAction = {};
        let tagStat = <></>;
        let tagEditButtons = <></>;

        //No tags, add indicator card
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

        //Otherwise, display the tag list
        } else if (hasAnyTags) {

            tagStat = (
                <div className={cellClasses} style={cellStyle}>
                    {
                        tags.map((tag) => {
                            const isActive = (this.state.activeTagID === tag.hashId);
                            const tagButtonID= `tag_button_${this.props.flight.id}_${tag.hashId}`;
                            const tagImageID = `tag_img_${this.props.flight.id}_${tag.hashId}`;
                            return (
                                <button
                                    id={tagButtonID}
                                    key={tag.hashId}
                                    className={`${buttonClasses} ${isActive ? 'active' : ''}`}
                                    onClick={() => this.selectTag(tag)}
                                >
                                    <i
                                        id={tagImageID}
                                        className="fa fa-tag m-1"
                                        data-bs-toggle="tooltip"
                                        data-bs-trigger='manual'
                                        data-bs-placement="right"
                                        style={{color : tag.color, marginRight : '10px'}}
                                    />
                                    {tag.name}
                                </button>
                            );
                        })
                    }
                </div> 
            );

            tagEditButtons = (
                <div className="flex flex-row items-center justify-start gap-1 m-1">
                    <button
                        className={`${buttonClasses}`}
                        title="Remove the selected tag from this flight"
                        onClick={() => this.removeTag()}
                        disabled={noTagSelected}
                    >
                        <i className="fa fa-minus" aria-hidden="true"></i>
                        <div>Remove Selected Tag</div>
                    </button>
                    <button
                        className={`${buttonClasses}`}
                        title="Permanently delete the selected tag from all flights"
                        onClick={() => {
                            this.deleteTag();
                        }}
                        disabled={noTagSelected}
                    >
                        <i className="fa fa-trash" aria-hidden="true"/>
                        <div>Delete Selected Tag</div>
                    </button>
                    <button
                        className={buttonClasses}
                        title="Clear all the tags from this flight"
                        onClick={() => {
                            this.props.clearTags(this.props.flight.id);
                            this.clearActiveTag();
                            this.setState({
                                editing : false,
                                addFormActive : false
                            });
                        }}
                    >
                        <i className="fa fa-eraser" aria-hidden="true"/>
                        <div>Clear All Tags</div>
                    </button>
                </div>
            );

        }

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


        let submitButton = <></>;
        if (this.state.editing || this.state.adding) {// && this.tagEquals(activeTag, editedTag)) {

            const editedTagHasName = (this.state.editedTag.name.length > 0);
            const editedTagHasDescription = (this.state.editedTag.description.length > 0);

            submitButton = (
                <button
                    id="submit-tag-button"
                    className="btn btn-outline-secondary"
                    data-bs-toggle="tooltip"
                    data-bs-trigger='manual'
                    data-bs-placement="top"
                    style={styleButton}
                    onClick={defAddAction}
                    disabled={!editedTagHasName || !editedTagHasDescription}
                >
                    <i className="fa fa-check mr-1" aria-hidden="true"/>
                    Submit
                </button> 
            );

        }

        const addDropButtonID = `dropdownMenuButton-${this.props.flight.id}`;

        const addDrop = (
            <div id="dropdown-item-button-add-tag" className="dropdown m-1">

                <button
                    ref={(element) => (this._addTagButton = element)}
                    className="btn btn-outline-secondary dropdown-toggle"
                    type="button"
                    id={addDropButtonID}
                    data-bs-toggle="dropdown"
                    data-bs-offset="0,8"
                    data-bs-config='{"boundary":"viewport","popperConfig":{"strategy":"fixed"}}'
                >
                    Add a Tag
                </button>

                <div
                    className="dropdown-menu"
                    style={{ maxHeight: "256px", overflowY: "auto", zIndex: 2000 }}
                    aria-labelledby={addDropButtonID}
                >
                {/* <div className="dropdown-menu absolute! top-full z-9999!" style={{maxHeight: "256px", overflowY: 'auto'}}> */}

                    {/* Create New Tag Button */}
                    <button
                        className="btn dropdown-item italic"
                        onClick={() => this.createClicked()}
                    >
                        + Create New Tag
                    </button>
                    {unassociatedTags != null && unassociatedTags.length > 0 &&
                        <div className="dropdown-divider"/>
                    }
                    {unassociatedTags != null && unassociatedTags.length > 0 &&
                        unassociatedTags.map((tag, index) => {
                            const style = {
                                backgroundColor : tag.color,
                                fontSize : "110%"
                            };
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
                                 <button type="button" className="btn input-group-text" title="Assign a color to this tag" onClick={() => $("#color-picker-tag").click()}>
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

                {/* Area Header */}
                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"} style={{marginBottom:"0"}}>
                    <div className="d-flex flex-column mr-3" style={{width: "16px", minWidth:"16px", maxWidth:"16px", height: "16px"}}>
                        <i className='fa fa-plus ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Tags
                    </div>
                </b>

                {tagEditButtons}
                {tagStat}

                <div className="flex-row m-1 mb-2">
                    {addDrop}{addForm}
                </div>
            </div>
        );
    }
}


document.addEventListener('show.bs.dropdown', e => console.log('show', e.target));
document.addEventListener('shown.bs.dropdown', e => console.log('shown', e.target));

export { Tags };