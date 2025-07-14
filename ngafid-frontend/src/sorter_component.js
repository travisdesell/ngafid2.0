import 'bootstrap';
import React from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';


class PaginationSorter extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const currentColumnName = this.props.getSortingColumn();
        const currentOrder = this.props.getSortingOrder();

        if (typeof this.props.sortOptions != 'undefined') {
            console.log("rendering pagination sorter!");
            return (
                <div className="col form-row input-group m-0 p-0">
                    <DropdownButton className="mr-0" id="dropdown-item-button-sort" title={
                        <span style={{ overflow: "hidden", maxWidth: "8vw", textOverflow: "ellipsis", whiteSpace: "nowrap", display: "inline-block", verticalAlign: "-25%"}}>
                            Sorting by: {currentColumnName}
                        </span>
                    } size="sm">
                    {
                        this.props.sortOptions.map((ruleInfo, index) => {
                            return (
                                <Dropdown.Item as="button" key={index} onClick={() => this.props.setSortingColumn(ruleInfo)}>{ruleInfo}</Dropdown.Item>
                            );
                        })
                    }
                    </DropdownButton>

                    <DropdownButton className="ml-2 mr-2" id="dropdown-item-button-resize" title={
                        <span style={{ overflow: "hidden", maxWidth: "8vw", textOverflow: "ellipsis", whiteSpace: "nowrap", display: "inline-block", verticalAlign: "-25%"}}>
                            {currentOrder} Order
                        </span>
                    } size="sm">
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
