// Mirrors the Kotlin data classes in app/src/main/java/com/mackson/delivery/data/model/ — same
// Firestore documents, read from the web instead of the Android app.

export type OrderStatus =
  | "RECEIVED"
  | "PICKING"
  | "READY_FOR_COLLECTION"
  | "EN_ROUTE"
  | "ARRIVED_AT_NODE"
  | "DELIVERED"
  | "CANCELLED";

export interface OrderItem {
  productId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  aisleNumber?: number;
  barcode?: string;
  isSubstituted?: boolean;
  substitutionApproved?: boolean | null;
}

export interface Order {
  orderId: string;
  customerId: string;
  storeId: string;
  shopperId?: string | null;
  driverId?: string | null;
  orderStatus: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  loyaltyDiscount: number;
  driverTip: number;
  serviceFee: number;
  totalAmount: number;
  deliverySlotLabel?: string;
  placementTime: number;
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
}

export interface Customer {
  customerId: string;
  name: string;
  email: string;
  mobileNumber: string;
  loyaltyPoints: number;
  loyaltyCardNumber?: string | null;
}

export interface DriverProfile {
  driverId: string;
  driverName: string;
  vehicleType: string;
  licensePlate?: string;
  photoUrl?: string;
  currentPayoutBalance: number;
  isAvailable: boolean;
}

export interface PickerProfile {
  shopperId: string;
  staffName: string;
  assignedStoreId: string;
  dutyStatus: "ON_DUTY" | "OFF_DUTY";
}

export interface StoreNode {
  storeId: string;
  branchName: string;
  address?: string;
  imageUrl?: string;
  latitude: number;
  longitude: number;
  serviceRadiusKm: number;
  isAcceptingOrders: boolean;
}
