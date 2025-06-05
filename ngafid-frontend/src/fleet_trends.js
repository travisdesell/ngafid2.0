import ReactDOM from "react-dom";
import React from "react";
import TrendsPage from "./trends";

const container = document.querySelector("#trends-page");
const root = ReactDOM.createRoot(container);
root.render(<TrendsPage aggregate_page={false}/>);