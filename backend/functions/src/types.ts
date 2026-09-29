/**
 * Shared types mirroring the JSON schemas documented in Part 1, section 9.3.11
 * (orders Document Schema / substitutionChats Document Schema) and the Android
 * Kotlin data classes in app/src/main/java/com/mackson/delivery/data/model.
 */

export type OrderStatus =
  | "RECEIVED"
  | "PICKING"
  | "READY_FOR_COLLECTION"
  | "EN_ROUTE"
  | "ARRIVED_AT_NODE"
  | "DELIVERED"
  | "CANCELLED";

export type UserRole = "CUSTOMER" | "PICKER" | "DRIVER" | "MANAGER" | "ADMIN";

export interface OrderItem {
  productId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  aisleNumber: number;
  barcode: string;
  imageUrl: string;
  isSubstituted: boolean;
  substitutionApproved: boolean | null;
}

export interface DeliveryAddress {
  addressId: string;
  label: string;
  latitude: number;
  longitude: number;
  formattedAddress: string;
  withinServiceRadius: boolean;
}

export interface Order {
  orderId: string;
  customerId: string;
  storeId: string;
  shopperId: string | null;
  driverId: string | null;
  orderStatus: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  loyaltyDiscount: number;
  driverTip: number;
  serviceFee: number;
  totalAmount: number;
  deliverySlotType: "IMMEDIATE_ROLLING" | "SCHEDULED_HOURLY";
  deliverySlotLabel: string;
  deliveryAddress: DeliveryAddress | null;
  paymentReferenceToken: string | null;
  placementTime: number;
  scheduledTime: number | null;
  deliveredTime: number | null;
  deliveryOtp?: string;
  deliveryQrToken?: string;
  /** True once store staff/admin have visually matched the driver's dispatch code against the
   * physical order at the counter — the driver app gates "Mark as collected" on this so a
   * driver can't self-declare a pickup with no staff sign-off (WIL group requirement). */
  dispatchConfirmedByAdmin?: boolean;
}

export interface Product {
  productId: string;
  storeId: string;
  name: string;
  description: string;
  unitPrice: number;
  weightGrams: number;
  category: string;
  aisleNumber: number;
  imageUrl: string;
  barcode: string;
  currentStockLevel: number;
  isAgeRestricted: boolean;
  nameSearchKey?: string;
}

export interface CheckoutRequestItem {
  productId: string;
  quantity: number;
}

export interface CheckoutRequest {
  storeId: string;
  items: CheckoutRequestItem[];
  deliverySlotType: "IMMEDIATE_ROLLING" | "SCHEDULED_HOURLY";
  deliverySlotLabel: string;
  applyLoyaltyDiscount: boolean;
  driverTip: number;
  voucherAmount?: number;
}
