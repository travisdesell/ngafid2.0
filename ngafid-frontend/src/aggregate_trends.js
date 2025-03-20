import TrendsPage from "./trends";
import ReactDOM from "react-dom";
import React from "react";

var aggregateTrendsPage = ReactDOM.render(
    <TrendsPage
        aggregate_page ={true}
    />,
    document.querySelector('#trends-page')
);

aggregateTrendsPage.displayPlots("All Airframes");