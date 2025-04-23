import ReactDOM from "react-dom";
import React from "react";

import SummaryPage from "./summary_page.js"


var page = ReactDOM.render(
    <SummaryPage aggregate={true}/>,
    document.querySelector("#aggregate-page")
);
