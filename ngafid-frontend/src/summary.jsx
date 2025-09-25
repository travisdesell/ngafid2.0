import { createRoot } from 'react-dom/client';
import React from "react";

import {SummaryPage} from "./summary_page.js";

const container = document.querySelector("#summary-page");
const root = createRoot(container);
root.render(<SummaryPage aggregate={false}/>);