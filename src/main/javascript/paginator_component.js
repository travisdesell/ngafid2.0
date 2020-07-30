import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';


class Paginator extends React.Component {
    constructor(props) {
        super(props);

        this.previousPage = this.previousPage.bind(this);
        this.nextPage = this.nextPage.bind(this);
        this.repaginate = this.repaginate.bind(this);
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
        this.props.submitFilter();
    }

    /**
     * Generates an array representing all the pages in this collection of 
     * queried flights
     * @return an array of String objects containing page names
     */
    genPages() {
        var page = [];
        for(var i = 0; i < this.props.numberPages; i++) {
            page.push({
                value : i,
                name : "Page "+(i+1)
            });
        }
        return page;
    }

    render() {
        let pages = this.genPages();

        var begin = this.props.currentPage == 0;
        var end = this.props.currentPage == this.props.numberPages-1;
        var prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
            var next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>

            if (begin) {
                prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
            }
        if (end) {
            next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
        }


        if (typeof this.props.items != 'undefined') {
            return (
                <div className="card mb-1 border-secondary">
                    <div className="p-2">
                        <button className="btn btn-sm btn-info pr-2" disabled>Page: {this.props.currentPage + 1} of {this.props.numberPages}</button>
                        <div className="btn-group mr-1 pl-1" role="group" aria-label="First group">
                            <DropdownButton  className="pr-1" id="dropdown-item-button" title={this.props.pageSize+ " " + this.props.itemName + " per page"} size="sm">
                                <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 {this.props.itemName} per page</Dropdown.Item>
                                <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 {this.props.itemName} per page</Dropdown.Item>
                                <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 {this.props.itemName} per page</Dropdown.Item>
                                <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 {this.props.itemName} per page</Dropdown.Item>
                                <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 {this.props.itemName} per page</Dropdown.Item>
                            </DropdownButton>
                            <Dropdown className="pr-1">
                                <Dropdown.Toggle variant="primary" id="dropdown-basic" size="sm">
                                    {"Page " + (this.props.currentPage + 1)}
                                </Dropdown.Toggle>
                                <Dropdown.Menu  style={{ maxHeight: "256px", overflowY: 'scroll' }}>
                                    {
                                        pages.map((pages, index) => {
                                            return (
                                                <Dropdown.Item key={index} as="button" onClick={() => this.jumpPage(pages.value)}>{pages.name}</Dropdown.Item>
                                            );
                                        })
                                    }
                                </Dropdown.Menu>
                            </Dropdown>
                            {prev}
                            {next}
                        </div>

                    </div>
                </div>
            );
        } else {
            return ( <div></div> );
        }
    }
}


export { Paginator };
