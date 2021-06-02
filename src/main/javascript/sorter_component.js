import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Pagination from 'react-bootstrap/Pagination';
import Form from 'react-bootstrap/Form';
import FormCheck from 'react-bootstrap/FormCheck';


class PaginationSorter extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        var currentColumnName = this.props.getSortingColumn();
        var currentOrder = this.props.getSortingOrder();

        if (typeof this.props.sortOptions != 'undefined') {
            console.log("rendering pagination sorter!");
            return (
                <div className="col form-row input-group m-0 p-0">
                    <DropdownButton className="ml-auto mr-0" id="dropdown-item-button-sort" title={"Sorting by: " + currentColumnName} size="sm">
                    {
                        this.props.sortOptions.map((ruleInfo, index) => {
                            return (
                                <Dropdown.Item as="button" key={index} onClick={() => this.props.setSortingColumn(ruleInfo)}>{ruleInfo}</Dropdown.Item>
                            )
                        })
                    }
                    </DropdownButton>

                    <DropdownButton className="ml-2 mr-2" id="dropdown-item-button-resize" title={currentOrder + " Order"} size="sm">
                        <Dropdown.Item as="button" onClick={() => this.props.setSortingOrder("Ascending")}>Ascending Order</Dropdown.Item>
                        <Dropdown.Item as="button" onClick={() => this.props.setSortingOrder("Descending")}>Descending Order</Dropdown.Item>
                    </DropdownButton>
                </div>
            );
        } else {
            return ( <div></div> );
        }
    }
}


export { PaginationSorter };
