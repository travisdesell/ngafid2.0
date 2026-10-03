import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";

import { InfoHint } from "./info_hint";

// Example component test using React Testing Library. Mirror this pattern for
// other components: render the component, then assert on what the user sees
// (text, roles) rather than implementation details.
describe("InfoHint", () => {
  it("renders the provided message text", () => {
    render(<InfoHint message="Heads up" />);
    expect(screen.getByText("Heads up")).toBeInTheDocument();
  });

  it("renders the info icon alongside the message", () => {
    const { container } = render(<InfoHint message="Something to note" />);
    expect(container.querySelector("i.fa-info-circle")).not.toBeNull();
  });
});
