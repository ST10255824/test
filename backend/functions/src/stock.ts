/**
 * Pure stock-guard logic, extracted from checkout.ts so it can be unit tested without a
 * Firestore transaction in the loop. The transaction in checkout.ts calls
 * `assertSufficientStock` against live Firestore reads — this is what prevents the race
 * condition described in Part 1 section 2.3 (two customers buying the last unit).
 */

export interface StockCheckLine {
  productId: string;
  requestedQuantity: number;
  currentStockLevel: number;
  productName: string;
}

export class InsufficientStockError extends Error {
  constructor(public readonly productName: string, public readonly available: number) {
    super(`Only ${available} of ${productName} left in stock`);
  }
}

export function assertSufficientStock(lines: StockCheckLine[]): void {
  for (const line of lines) {
    if (line.requestedQuantity <= 0) {
      throw new Error(`Requested quantity for ${line.productName} must be at least 1`);
    }
    if (line.requestedQuantity > line.currentStockLevel) {
      throw new InsufficientStockError(line.productName, line.currentStockLevel);
    }
  }
}
