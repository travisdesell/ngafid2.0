/**
 * Global Vitest setup: registers the @testing-library/jest-dom matchers
 * (`toBeInTheDocument`, `toHaveTextContent`, ...) on Vitest's `expect`, and runs
 * React Testing Library's automatic cleanup after each test.
 */
import "@testing-library/jest-dom/vitest";
