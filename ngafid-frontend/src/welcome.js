import ReactDOM from "react-dom";
import React from "react";

import {SummaryPage} from "./summary_page.js";


const container = document.querySelector("#welcome-page");
const root = ReactDOM.createRoot(container);
root.render(<SummaryPage aggregate={false}/>);