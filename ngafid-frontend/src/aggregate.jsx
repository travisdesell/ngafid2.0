// ngafid-frontend/src/aggregate.jsx
import React from "react";
import {createRoot} from 'react-dom/client';

import {SummaryPage} from "./summary_page.tsx";

const container = document.querySelector("#aggregate-page");
const root = createRoot(container);
root.render(<SummaryPage aggregate={true}/>);