// ngafid-frontend/src/app/components/page_title.tsx

/*
    Provides 2 ways of setting the page (browser tab) title:

    1. The setPageTitle function, which can be called directly.
    2. The <PageTitle> component, which sets the page title when rendered.

    Either works; haven't decided which is better yet.
*/

import { getLogger } from "@/components/providers/logger";
import { useEffect } from "react";

const log = getLogger("PageTitle", "black", "Component");

interface PageTitleProps {
    title: string;
}

export function setPageTitle(title: string) {

    const newFullTitle = `${title} — NGAFID`;

    document.title = newFullTitle;
    log(`Set page title to: '${newFullTitle}'`);

}

export default function PageTitle({ title }: PageTitleProps) {

    useEffect(() => {
        setPageTitle(title);
    }, [title]);

    // Render nothing
    return <></>
    
}