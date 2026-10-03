import React from "react";

interface InfoHintProps {
  message?: string;
}

/**
 * Renders a subtle, dimmed informational hint row with an info icon and message text.
 *
 * @param props Component props.
 * @param props.message Optional hint text displayed next to the info icon.
 * @returns The rendered info hint element.
 */
export function InfoHint({ message }: InfoHintProps) {
  return (
    <div className="opacity-50 my-4 flex items-center">
      <i className="fa fa-info-circle mr-2" aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}

export default InfoHint;
