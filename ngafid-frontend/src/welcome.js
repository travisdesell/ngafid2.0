import ReactDOM from "react-dom";
import React from "react";

import SummaryPage from "./summary_page.js"

var welcomePage= ReactDOM.render(
    <SummaryPage aggregate={false}/>,
    document.querySelector('#welcome-page')
);
