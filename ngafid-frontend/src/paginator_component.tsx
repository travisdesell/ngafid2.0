import 'bootstrap';
import React from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Pagination from 'react-bootstrap/Pagination';
import {PaginationSorter} from './sorter_component.js';
import {showErrorModal} from "./error_modal.js";
import { UploadsPage } from './uploads.js';

type PaginatorProps = {
    numberPages: number,
    currentPage: number,
    items?: unknown[],
    doUploadButtonHide?: boolean,
    updateCurrentPage: (page: number) => void,
    updateItemsPerPage: (pageSize: number) => void,
    sortOptions?: unknown,
    setSortingColumn: (sortColumn: string) => void,
    getSortingColumn: () => string,
    setSortingOrder: (order: string) => void,
    getSortingOrder: () => string,
    pageSize?: number,
    itemName?: string,
    uploadsPage: UploadsPage,
    location?: unknown,
}

type PaginatorState = {
    goto_active: boolean,
    goto_value: number,
}

class Paginator extends React.Component<PaginatorProps, PaginatorState> {

    constructor(props: PaginatorProps) {
        super(props);

        this.state = {
            goto_active: false,
            goto_value: 1,
        };

        this.previousPage = this.previousPage.bind(this);
        this.nextPage = this.nextPage.bind(this);
        this.repaginate = this.repaginate.bind(this);
    }

    updateGoto(event: React.ChangeEvent<HTMLInputElement>) {

        const value = event.target.value;
        const valueNumeric = Number(value);
        console.log(`goto updated to: '${  valueNumeric  }'`);

        //Got valid value between 1 and numberPages
        if (/^\d+$/.test(value) && valueNumeric > 0 && valueNumeric <= this.props.numberPages) {
            console.log("Paginator - Setting goto_active to true!");
            this.setState({
                goto_active: true,
                goto_value: valueNumeric
            });

        //Otherwise, got invalid value
        } else {
            console.log("Paginator - Setting goto_active to false!");
            this.setState({
                goto_active: false
            });
        }
        
    }

    /**
     ** Jumps to a page in this collection of queried flights
     ** @param pg the page to jump to
     **/
    jumpPage(page: number) {

        if (page < this.props.numberPages && page >= 0)
            this.props.updateCurrentPage(page);
        
    }

    /**
     *    * jumps to the next page in this collection of queried flights
     *        */
    nextPage() {

        const newPageIndex = (this.props.currentPage + 1);
        this.props.updateCurrentPage(newPageIndex);

    }

    /**
     ** jumps to the previous page in this collection of queried flights
     **/
    previousPage() {

        const newPageIndex = (this.props.currentPage - 1);
        this.props.updateCurrentPage(newPageIndex);

    }

    /**
     ** Repaginates the page configuration when the numPerPage field has been changed by the user
     **/
    repaginate(pageSize: number) {

        console.log("Re-Paginating");
        this.props.updateItemsPerPage(pageSize);

    }

    triggerInput() {
        console.log("Paginator - Input triggered!");

        const uploadsPageRef = this.props.uploadsPage;

        $('#upload-file-input').trigger('click');

        $('#upload-file-input:not(.bound)').addClass('bound').change(function (this: HTMLElement) {

            const input = this as HTMLInputElement;

            //Got no files, exit
            if (!input.files || input.files.length === 0)
                return;

            console.log(`Paginator - Got ${input.files.length} files selected:`, input.files);
            const file = input.files[0];
            const filename = file.webkitRelativePath || file.name;

            const isZip = file['type'].includes("zip");
            const isParquet = filename.endsWith(".parquet");
            console.log(`Paginator - isZip: ${isZip}, isParquet: ${isParquet}`);

            //Bad file name -> Error
            if (!filename.match(/^[a-zA-Z0-9_.-]*$/)) {
                showErrorModal("Malformed Filename", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");

            //Bad file type -> Error
            } else if (!isZip && !isParquet) {
                showErrorModal("Malformed Filename", "Uploaded files must be zip files or a parquet file. The zip file should contain directories which contain flight logs (csv files). The directories should be named for the tail number of the airfraft that generated the flight logs within them.");

            //All good, add the upload
            } else {
                uploadsPageRef.addUpload(file);
            }

        });
    }

    render() {
        const pages = [];

        const totalVisiblePages = 9;
        const pagesAround = 4;

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
                <Pagination.Item key={pageNumber} onClick={() => this.jumpPage(pageNumber)}
                                 active={pageNumber === this.props.currentPage}>{pageNumber + 1}</Pagination.Item>
            );
        }

        /*
        if ((this.props.currentPage + pagesAround) < (this.props.numberPages - 2)) {
            pages.push(
                <Pagination.Ellipsis key="end_ellipsis" disabled />
            );
        }
        */

        let sorter = null;
        if (this.props.sortOptions != null) {
            sorter = (
                <PaginationSorter
                    sortOptions={this.props.sortOptions}
                    setSortingColumn={(sortColumn: string) => this.props.setSortingColumn(sortColumn)}
                    getSortingColumn={() => this.props.getSortingColumn()}
                    setSortingOrder={(order: string) => this.props.setSortingOrder(order)}
                    getSortingOrder={() => this.props.getSortingOrder()}
                />
            );
        }

        let numTotalPages = this.props.numberPages;

        //Require minimum of 1 page
        if (this.props.numberPages == 0)
            numTotalPages = 1;

        const PAGE_FLIGHTS = (window.location.pathname === "/protected/flights");

        return (
            <div className="card border-secondary"
                 style={{
                     backgroundColor: "var(--c_bg)",
                     textAlign: "center",
                     width: "100%",
                     userSelect: "none",
                 }}
            >

            	<div className="row m-0 p-2 d-flex flex-column">


                	<div className="d-flex flex-row flex-wrap" style={{rowGap:"0.50em"}}>

                        <div className="row" style={{alignContent: "start", marginLeft: "0em", marginRight: "auto"}}>

                            <div className="btn btn-sm btn-info mr-2 pointer-events-none">
                                Page: {this.props.currentPage + 1} of {numTotalPages}
                            </div>

                            <Pagination size="sm" className="m-0 mr-2">
                                <Pagination.First disabled={this.props.currentPage === 0}
                                                  onClick={() => this.jumpPage(0)}/>
                                <Pagination.Prev disabled={this.props.currentPage === 0}
                                                 onClick={() => this.previousPage()}/>

                                {pages}

                                <Pagination.Next disabled={this.props.currentPage >= (this.props.numberPages - 1)}
                                                 onClick={() => this.nextPage()}/>
                                <Pagination.Last disabled={this.props.currentPage >= (this.props.numberPages - 1)}
                                                 onClick={() => this.jumpPage(this.props.numberPages - 1)}/>
                            </Pagination>

                            <div className="col form-row input-group m-0 p-0" style={{width: "160px"}}>
                                <div className="input-group-prepend p-0" style={{
                                    display: "flex",
                                    flexFlow: "column wrap",
                                    minWidth: "35%",
                                    maxWidth: "35%"
                                }}>
                                    <button className="btn btn-sm btn-primary" disabled={!this.state.goto_active}
                                            onClick={() => this.jumpPage(this.state.goto_value - 1)}>
                                        Go To
                                    </button>
                                </div>
                                <input id="jump-text" type="text" className="form-control col-2" placeholder="Page"
                                       style={{height: "31px", minWidth: "65%", maxWidth: "65%"}} onChange={(event) => {
                                    this.updateGoto(event);
                                }}></input>
                            </div>
                        </div>

                        {/* RIGHT ELEMENTS */}
                        <div className={`d-flex flex-row ${  PAGE_FLIGHTS ? "" : "ml-auto"}`}
                             style={{justifyContent: "space-between", flexDirection: "row", alignContent: "end"}}>

                            {
                                //Show Upload Flights button on the Uploads page
                                (window.location.pathname === "/protected/uploads" && !this.props.doUploadButtonHide)
                                &&
                                <button id="upload-flights-button" className="btn btn-primary btn-sm float-right mr-2"
                                        onClick={() => this.triggerInput()}>
                                    <i className="fa fa-upload"></i> Upload Flights
                                </button>
                            }

                            <div className="d-flex flex-row ml-auto">

                                {sorter}

                                <DropdownButton id="dropdown-item-button-resize" title={
                                    <span style={{
                                        overflow: "hidden",
                                        maxWidth: "8vw",
                                        textOverflow: "ellipsis",
                                        whiteSpace: "nowrap",
                                        display: "inline-block",
                                        verticalAlign: "-25%"
                                    }}>
                                    	{`${this.props.pageSize  } ${  this.props.itemName  } per page`}
                                	</span>
                                } size="sm">
                                    <Dropdown.Item as="button"
                                                   onClick={() => this.repaginate(10)}>10 {this.props.itemName} per
                                        page</Dropdown.Item>
                                    <Dropdown.Item as="button"
                                                   onClick={() => this.repaginate(15)}>15 {this.props.itemName} per
                                        page</Dropdown.Item>
                                    <Dropdown.Item as="button"
                                                   onClick={() => this.repaginate(25)}>25 {this.props.itemName} per
                                        page</Dropdown.Item>
                                    <Dropdown.Item as="button"
                                                   onClick={() => this.repaginate(50)}>50 {this.props.itemName} per
                                        page</Dropdown.Item>
                                    <Dropdown.Item as="button"
                                                   onClick={() => this.repaginate(100)}>100 {this.props.itemName} per
                                        page</Dropdown.Item>
                                </DropdownButton>
                            </div>

                        </div>

                    </div>

                </div>
            </div>
        );

    }
}


export {Paginator};