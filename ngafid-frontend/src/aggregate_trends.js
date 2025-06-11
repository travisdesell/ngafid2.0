import TrendsPage from "./trends";
import React from "react";
import { createRoot } from "react-dom/client";

const container = document.querySelector("#trends-page");
const root = createRoot(container);
root.render(<TrendsPage aggregate_page={true}/>);