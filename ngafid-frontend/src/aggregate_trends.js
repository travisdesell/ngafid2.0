import TrendsPage from "./trends";
import ReactDOM from "react-dom";
import React from "react";

const container = document.querySelector("#trends-page");
const root = ReactDOM.createRoot(container);
const aggregateTrendsPage = root.render(<TrendsPage aggregate_page={true}/>);

aggregateTrendsPage.displayPlots("All Airframes");