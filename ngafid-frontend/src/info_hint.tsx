import React from "react";

interface InfoHintProps {
    message?: string;
}

export function InfoHint({message}: InfoHintProps) {
    return (
        <div className="opacity-50 my-4 flex items-center">
            <i className="fa fa-info-circle mr-2" aria-hidden="true" />
            <span>{message}</span>
        </div>
    );
};

export default InfoHint;
