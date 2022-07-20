import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Pagination from 'react-bootstrap/Pagination';
import Form from 'react-bootstrap/Form';
import FormCheck from 'react-bootstrap/FormCheck';
import { PaginationSorter } from './sorter_component.js';
import {cesiumFlightsSelected} from "./flight_component";


class Paginator extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            goto_active : false,
            goto_value : 1,
            clear_flights_active: cesiumFlightsSelected.length === 0,
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
            this.props.submitFilter();
        }
    }

    /**
     *    * jumps to the next page in this collection of queried flights
     *        */
    nextPage() {
        this.props.updateCurrentPage(this.props.currentPage + 1);
        this.props.submitFilter();
    }

    /**
     ** jumps to the previous page in this collection of queried flights
     **/
    previousPage() {
        this.props.updateCurrentPage(this.props.currentPage - 1);
        this.props.submitFilter();
    }

    /**
     ** Repaginates the page configuration when the numPerPage field has been changed by the user
     **/
    repaginate(pageSize) {
        console.log("Re-Paginating");
        this.props.updateItemsPerPage(pageSize);
        this.props.submitFilter(true);
    }

     /**
     * Handles clearing all selected flights for multiple flight replays
     */
    clearCesiumFlights() {
        while (cesiumFlightsSelected.length !== 0) {
            let removedFlight = cesiumFlightsSelected.pop()
            console.log("Removed " + removedFlight);
            document.getElementById("cesiumToggled" + cesiumFlightsSelected.pop()).setAttribute("aria-pressed", "false");
        }
        this.state.clear_flights_active = false;
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

        if (typeof this.props.items != 'undefined') {
            return (
                <div className="card mb-1 border-secondary">
                    <div className="row m-0 p-2">
                        <button className="btn btn-sm btn-info mr-2" disabled>Page: {this.props.currentPage + 1} of {numTotalPages}</button>

                        <Pagination size="sm" className="m-0 mr-2">
                            <Pagination.First disabled={this.props.currentPage === 0} onClick={() => this.jumpPage(0)}/>
                            <Pagination.Prev disabled={this.props.currentPage === 0} onClick={() => this.previousPage()}/>

                            {pages}

                            <Pagination.Next disabled={this.props.currentPage === this.props.numberPages - 1} onClick={() => this.nextPage()} />
                            <Pagination.Last disabled={this.props.currentPage === this.props.numberPages - 1} onClick={() => this.jumpPage(this.props.numberPages - 1)} />
                        </Pagination>

                        <div className="col form-row input-group m-0 p-0">
                            <div className="input-group-prepend p-0">
                                <button className="btn btn-sm btn-primary" disabled={!this.state.goto_active} onClick={() => this.jumpPage(this.state.goto_value - 1)}>Go To</button>
                            </div>
                            <input id="jump-text" type="text" className="form-control col-2" placeholder="Page" style={{height:"31px"}} onChange={(event) => {this.updateGoto(event);}}></input>
                            <button className="btn btn-sm btn-primary" disabled={!this.state.clear_flights_active} onClick={() => this.clearCesiumFlights()}>Clear Selected Replays</button>
                        </div>


                        {sorter}

                        <DropdownButton className="ml-auto mr-2" id="dropdown-item-button-resize" title={this.props.pageSize+ " " + this.props.itemName + " per page"} size="sm">
                            <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 {this.props.itemName} per page</Dropdown.Item>
                            <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 {this.props.itemName} per page</Dropdown.Item>
                            <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 {this.props.itemName} per page</Dropdown.Item>
                            <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 {this.props.itemName} per page</Dropdown.Item>
                            <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 {this.props.itemName} per page</Dropdown.Item>
                        </DropdownButton>

                    </div>
                </div>
            );
        } else {
            return ( <div></div> );
        }
    }
}


export { Paginator };
