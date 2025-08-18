import { createRoot } from 'react-dom/client';
import React from "react";
import { TrendsPage } from './trends';

const container = document.querySelector("#trends-page");
if (container) {
    const root = createRoot(container);
    root.render(<TrendsPage aggregate_page={false}/>);
}