import { describe, expect, it } from "vitest";
import { isOwnPieceToken, tokenColor } from "./tokens";

describe("токены доски", () => {
  it("определяет цвет простых шашек и дамок", () => {
    expect(tokenColor("w")).toBe("WHITE");
    expect(tokenColor("W")).toBe("WHITE");
    expect(tokenColor("b")).toBe("BLACK");
    expect(tokenColor("B")).toBe("BLACK");
    expect(tokenColor("")).toBeNull();
  });

  it("распознаёт свою простую шашку (нижний регистр)", () => {
    expect(isOwnPieceToken("w", "WHITE")).toBe(true);
    expect(isOwnPieceToken("b", "BLACK")).toBe(true);
    expect(isOwnPieceToken("w", "BLACK")).toBe(false);
    expect(isOwnPieceToken("b", "WHITE")).toBe(false);
  });

  it("распознаёт свою дамку (верхний регистр)", () => {
    expect(isOwnPieceToken("W", "WHITE")).toBe(true);
    expect(isOwnPieceToken("B", "BLACK")).toBe(true);
    expect(isOwnPieceToken("", "WHITE")).toBe(false);
  });
});
