/**
 * Pure checkout arithmetic — the server-side source of truth. Mirrors
 * app/src/main/java/com/mackson/delivery/domain/CheckoutCalculator.kt so both the Android
 * preview (before payment) and this authoritative calculation agree, but the client's numbers
 * are never trusted: checkout.ts recomputes everything here from Firestore-read prices.
 */

const SERVICE_FEE_RATE = 0.02;
const MIN_SERVICE_FEE = 5.0;
const LOYALTY_DISCOUNT_RATE = 0.05;

export interface PricedLine {
  productId: string;
  quantity: number;
  unitPrice: number;
}

export interface PriceBreakdown {
  subtotal: number;
  loyaltyDiscount: number;
  serviceFee: number;
  driverTip: number;
  total: number;
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

export function computeSubtotal(lines: PricedLine[]): number {
  return round2(lines.reduce((sum, line) => sum + line.unitPrice * line.quantity, 0));
}

export function computeCheckoutTotals(
  lines: PricedLine[],
  applyLoyaltyDiscount: boolean,
  driverTip = 0,
  voucherAmount = 0
): PriceBreakdown {
  if (driverTip < 0) throw new Error("Driver tip cannot be negative");
  if (voucherAmount < 0) throw new Error("Voucher amount cannot be negative");

  const subtotal = computeSubtotal(lines);
  const loyaltyDiscount = applyLoyaltyDiscount ? round2(subtotal * LOYALTY_DISCOUNT_RATE) : 0;
  const discountedSubtotal = Math.max(0, subtotal - loyaltyDiscount - voucherAmount);
  const serviceFee = discountedSubtotal <= 0 ? 0 : Math.max(MIN_SERVICE_FEE, round2(discountedSubtotal * SERVICE_FEE_RATE));
  const total = round2(discountedSubtotal + serviceFee + driverTip);

  return {
    subtotal,
    loyaltyDiscount,
    serviceFee,
    driverTip: round2(driverTip),
    total
  };
}
