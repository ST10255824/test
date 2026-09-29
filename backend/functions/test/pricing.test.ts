import { computeCheckoutTotals, computeSubtotal, PricedLine } from "../src/pricing";

const lines: PricedLine[] = [
  { productId: "p1", quantity: 2, unitPrice: 20.0 },
  { productId: "p2", quantity: 1, unitPrice: 18.5 }
];

describe("computeSubtotal", () => {
  it("sums quantity * unitPrice across all lines", () => {
    expect(computeSubtotal(lines)).toBe(58.5);
  });

  it("returns 0 for an empty cart", () => {
    expect(computeSubtotal([])).toBe(0);
  });
});

describe("computeCheckoutTotals", () => {
  it("applies no loyalty discount when not requested", () => {
    const breakdown = computeCheckoutTotals(lines, false);
    expect(breakdown.loyaltyDiscount).toBe(0);
  });

  it("applies a 5% loyalty discount off the subtotal", () => {
    const breakdown = computeCheckoutTotals(lines, true);
    expect(breakdown.loyaltyDiscount).toBeCloseTo(2.925, 1);
  });

  it("never charges a service fee below the R5 minimum", () => {
    const tinyCart: PricedLine[] = [{ productId: "p1", quantity: 1, unitPrice: 5.0 }];
    const breakdown = computeCheckoutTotals(tinyCart, false);
    expect(breakdown.serviceFee).toBe(5.0);
  });

  it("adds the driver tip on top of the total, untaxed", () => {
    const withTip = computeCheckoutTotals(lines, false, 10);
    const withoutTip = computeCheckoutTotals(lines, false, 0);
    expect(withTip.total).toBeCloseTo(withoutTip.total + 10, 5);
  });

  it("rejects a negative driver tip", () => {
    expect(() => computeCheckoutTotals(lines, false, -1)).toThrow();
  });

  it("rejects a negative voucher amount", () => {
    expect(() => computeCheckoutTotals(lines, false, 0, -5)).toThrow();
  });

  it("never lets a voucher push the discounted subtotal negative", () => {
    const breakdown = computeCheckoutTotals(lines, false, 0, 1000);
    expect(breakdown.serviceFee).toBe(0);
    expect(breakdown.total).toBe(0);
  });
});
