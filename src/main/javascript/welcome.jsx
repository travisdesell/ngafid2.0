import ReactDOM from "react-dom";
import React from "react";

import SummaryPage from "./summary_page.jsx"

var welcomePage= ReactDOM.render(
    <SummaryPage aggregate={false}/>,
    document.querySelector('#welcome-page')
);
