import ReactDOM from "react-dom";
import React from "react";
import TrendsPage from "./trends"

var trendsPage = ReactDOM.render(
    <TrendsPage
        aggregate_page ={false}
    />,
    document.querySelector('#trends-page')
);

trendsPage.displayPlots("All Airframes");