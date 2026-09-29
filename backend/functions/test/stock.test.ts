import { assertSufficientStock, InsufficientStockError, StockCheckLine } from "../src/stock";

describe("assertSufficientStock", () => {
  it("passes when every line has enough stock", () => {
    const lines: StockCheckLine[] = [
      { productId: "p1", requestedQuantity: 2, currentStockLevel: 5, productName: "Bread" }
    ];
    expect(() => assertSufficientStock(lines)).not.toThrow();
  });

  it("throws InsufficientStockError when a line exceeds live stock", () => {
    const lines: StockCheckLine[] = [
      { productId: "p1", requestedQuantity: 6, currentStockLevel: 5, productName: "Bread" }
    ];
    expect(() => assertSufficientStock(lines)).toThrow(InsufficientStockError);
  });

  it("reports the product name and available quantity in the error", () => {
    const lines: StockCheckLine[] = [
      { productId: "p1", requestedQuantity: 6, currentStockLevel: 5, productName: "Bread" }
    ];
    try {
      assertSufficientStock(lines);
      fail("expected assertSufficientStock to throw");
    } catch (error) {
      expect(error).toBeInstanceOf(InsufficientStockError);
      expect((error as InsufficientStockError).available).toBe(5);
      expect((error as InsufficientStockError).message).toContain("Bread");
    }
  });

  it("rejects a zero or negative requested quantity", () => {
    const lines: StockCheckLine[] = [
      { productId: "p1", requestedQuantity: 0, currentStockLevel: 5, productName: "Bread" }
    ];
    expect(() => assertSufficientStock(lines)).toThrow();
  });

  it("validates every line, not just the first", () => {
    const lines: StockCheckLine[] = [
      { productId: "p1", requestedQuantity: 1, currentStockLevel: 5, productName: "Bread" },
      { productId: "p2", requestedQuantity: 9, currentStockLevel: 3, productName: "Milk" }
    ];
    expect(() => assertSufficientStock(lines)).toThrow(/Milk/);
  });
});
