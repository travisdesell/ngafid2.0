import { describe, it, expect } from "vitest";

import { paletteAt, paletteGenerator } from "./map_utils";

// Example unit test for pure utility functions. Mirror this pattern for other
// non-UI helpers: import the function, assert its output for representative and
// boundary inputs. No DOM or React is involved here.
describe("paletteAt", () => {
  it("returns pure green at probability 0", () => {
    expect(paletteAt(0)).toEqual([0, 255, 0]);
  });

  it("returns yellow at the 0.8 green->yellow boundary", () => {
    expect(paletteAt(0.8)).toEqual([255, 255, 0]);
  });

  it("interpolates green->yellow for probabilities below 0.8", () => {
    expect(paletteAt(0.4)).toEqual([128, 255, 0]);
  });

  it("interpolates yellow->red for probabilities between 0.8 and 1.0", () => {
    expect(paletteAt(0.9)).toEqual([255, 128, 0]);
  });

  it("clamps to red at probability 1.0 and above", () => {
    expect(paletteAt(1.0)).toEqual([255, 0, 0]);
  });
});

describe("paletteGenerator", () => {
  const palette = paletteGenerator(
    [
      [0, 0, 0],
      [255, 255, 255],
    ],
    [0, 1],
  );

  it("returns the first stop at position 0", () => {
    expect(palette(0)).toEqual([0, 0, 0]);
  });

  it("interpolates midway between two stops", () => {
    expect(palette(0.5)).toEqual([128, 128, 128]);
  });

  it("returns the last stop at position 1", () => {
    expect(palette(1)).toEqual([255, 255, 255]);
  });
});
