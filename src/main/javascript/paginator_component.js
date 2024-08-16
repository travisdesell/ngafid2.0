import 'bootstrap';
import React, { Component } from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Pagination from 'react-bootstrap/Pagination';
import { PaginationSorter } from './sorter_component.js';
import {CesiumButtons} from "./cesium_buttons";


class Paginator extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            goto_active : false,
            goto_value : 1,
            // clear_flights_active: cesiumFlightsSelected.length === 0,
        };

        this.previousPage = this.previousPage.bind(this);
        this.nextPage = this.nextPage.bind(this);
        this.repaginate = this.repaginate.bind(this);
    }

    updateGoto(event) {
        let value = event.target.value;
        console.log("goto updated to: '" + value + "'");
        if (/^\d+$/.test(value) && value > 0 && value <= this.props.numberPages) {
            console.log("setting goto active to true!");
            this.setState({
                goto_active : true,
                goto_value : value
            });

        } else {
            console.log("setting goto active to false!");
            this.setState({
                goto_active : false
            });
        }
    }

    /**
     ** Jumps to a page in this collection of queried flights
     ** @param pg the page to jump to
     **/
    jumpPage(page) {
        if (page < this.props.numberPages && page >= 0){
            this.props.updateCurrentPage(page);

            if (typeof this.props.items === 'undefined')
                return;

            this.props.submitFilter();
        }
    }

    /**
     *    * jumps to the next page in this collection of queried flights
     *        */
    nextPage() {
        this.props.updateCurrentPage(this.props.currentPage + 1);

        if (typeof this.props.items === 'undefined')
            return;

        this.props.submitFilter();
    }

    /**
     ** jumps to the previous page in this collection of queried flights
     **/
    previousPage() {
        this.props.updateCurrentPage(this.props.currentPage - 1);

        if (typeof this.props.items === 'undefined')
            return;

        this.props.submitFilter();
    }

    /**
     ** Repaginates the page configuration when the numPerPage field has been changed by the user
     **/
    repaginate(pageSize) {

        console.log("Re-Paginating");
        this.props.updateItemsPerPage(pageSize);

        if (typeof this.props.items === 'undefined')
            return;

        this.props.submitFilter(true);
    }

    triggerInput() {
        console.log("input triggered!");

        let uploadsPageRef = this.props.uploadsPage;

        $('#upload-file-input').trigger('click');

        $('#upload-file-input:not(.bound)').addClass('bound').change(function() {
            console.log("number files selected: " + this.files.length);
            console.log( this.files );

            if (this.files.length > 0) {
                var file = this.files[0];
                var filename = file.webkitRelativePath || file.fileName || file.name;

                const isZip = file['type'].includes("zip");
                console.log("isZip: " + isZip);

                if (!filename.match(/^[a-zA-Z0-9_.-]*$/)) {
                    errorModal.show("Malformed Filename", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
                } else if (!isZip) {
                    errorModal.show("Malformed Filename", "Uploaded files must be zip files. The zip file should contain directories which contain flight logs (csv files). The directories should be named for the tail number of the airfraft that generated the flight logs within them.");
                } else {
                    uploadsPageRef.addUpload(file);
                }
            }
        });
    }

    render() {
        let pages = [];

        let totalVisiblePages = 9;
        let pagesAround = 4;

        let firstVisiblePage = this.props.currentPage - pagesAround;
        let lastVisiblePage = this.props.currentPage + pagesAround + 1;

        if (this.props.numberPages > totalVisiblePages) {
            if (firstVisiblePage < 0) {
                lastVisiblePage -= firstVisiblePage;
                firstVisiblePage -= firstVisiblePage;
            } else if (lastVisiblePage > this.props.numberPages) {
                firstVisiblePage -= (lastVisiblePage - this.props.numberPages);
                //lastVisiblePage -= (this.props.numberPages - lastVisiblePage);
            }
        }



        /*
        if ((this.props.currentPage - pagesAround) > 0) {
            pages.push(
                <Pagination.Ellipsis key="begin_ellipsis" disabled />
            );
        }
        */

        for (let pageNumber = firstVisiblePage; pageNumber < lastVisiblePage; pageNumber++) {
            if (pageNumber < 0 || pageNumber >= this.props.numberPages) continue;

            pages.push(
                <Pagination.Item key={pageNumber} onClick={() => this.jumpPage(pageNumber)} active={pageNumber === this.props.currentPage}>{pageNumber + 1}</Pagination.Item>
            );
        }

        /*
        if ((this.props.currentPage + pagesAround) < (this.props.numberPages - 2)) {
            pages.push(
                <Pagination.Ellipsis key="end_ellipsis" disabled />
            );
        }
        */

        var sorter = "";
        if (this.props.sortOptions != null) {
            sorter = (
                <PaginationSorter
                    sortOptions={this.props.sortOptions}
                    setSortingColumn={(sortColumn) => this.props.setSortingColumn(sortColumn)}
                    getSortingColumn={() => this.props.getSortingColumn()}
                    setSortingOrder={(order) => this.props.setSortingOrder(order)}
                    getSortingOrder={() => this.props.getSortingOrder()}
                />
            );
        }

        let numTotalPages = this.props.numberPages;
        if (this.props.numberPages == 0) {
            numTotalPages = 1;
        }

        let PAGE_FLIGHTS = (window.location.pathname === "/protected/flights");

        return (
            <div className="card border-secondary"
                style={{
                    backgroundColor:"var(--c_bg)",
                    textAlign: "center",
                    width: "100%",
                    userSelect: "none",
                }}
            >

                <div className="row m-0 p-2 d-flex flex-column">
                    
                    <div className="d-flex flex-row flex-wrap" style={{rowGap:"0.50em"}}>

                        <div className="row" style={{alignContent:"start", marginLeft:"0em", marginRight:"auto"}}>

                            <button className="btn btn-sm btn-info mr-2" disabled>
                                Page: {this.props.currentPage + 1} of {numTotalPages}
                            </button>

                            <Pagination size="sm" className="m-0 mr-2">
                                <Pagination.First disabled={this.props.currentPage === 0} onClick={() => this.jumpPage(0)}/>
                                <Pagination.Prev disabled={this.props.currentPage === 0} onClick={() => this.previousPage()}/>

                                {pages}

                                <Pagination.Next disabled={this.props.currentPage >= (this.props.numberPages-1)} onClick={() => this.nextPage()} />
                                <Pagination.Last disabled={this.props.currentPage >= (this.props.numberPages-1)} onClick={() => this.jumpPage(this.props.numberPages - 1)} />
                            </Pagination>

                            <div className="col form-row input-group m-0 p-0" style={{width:"160px"}}>
                                <div className="input-group-prepend p-0" style={{display:"flex", flexFlow:"column wrap", minWidth:"35%", maxWidth:"35%"}}>
                                    <button className="btn btn-sm btn-primary" disabled={!this.state.goto_active} onClick={() => this.jumpPage(this.state.goto_value - 1)}>
                                        Go To
                                    </button>
                                </div>
                                <input id="jump-text" type="text" className="form-control col-2" placeholder="Page" style={{height:"31px", minWidth:"65%", maxWidth:"65%"}} onChange={(event) => {this.updateGoto(event);}}></input>
                            </div>
                        </div>

                        {/* RIGHT ELEMENTS */}
                        <div className={"d-flex flex-row "+(PAGE_FLIGHTS?"":"ml-auto")} style={{justifyContent:"space-between", flexDirection:"row", alignContent:"end"}}>

                            {
                                //Show Upload Flights button on the Uploads page
                                (window.location.pathname === "/protected/uploads") &&
                                <button id="upload-flights-button" className="btn btn-primary btn-sm float-right mr-2" onClick={() => this.triggerInput()}>
                                    <i className="fa fa-upload"></i> Upload Flights
                                </button>
                            }

                            <div className="d-flex flex-row ml-auto"> 
                                
                                {sorter}

                                <DropdownButton id="dropdown-item-button-resize" title={
                                    <span style={{ overflow: "hidden", maxWidth: "8vw", textOverflow: "ellipsis", whiteSpace: "nowrap", display: "inline-block", verticalAlign: "-25%"}}>
                                        {this.props.pageSize+ " " + this.props.itemName + " per page"}
                                    </span>
                                    } size="sm">
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 {this.props.itemName} per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 {this.props.itemName} per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 {this.props.itemName} per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 {this.props.itemName} per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 {this.props.itemName} per page</Dropdown.Item>
                                </DropdownButton>
                            </div>


                            {
                            //Show Cesium buttons on the Flights page
                            (PAGE_FLIGHTS) &&
                            <div className="ml-4">
                                <CesiumButtons location={this.props.location}/>
                            </div>
                            }

                        </div>

                    </div>

                </div>
            </div>
        );

    }
}


export { Paginator };
