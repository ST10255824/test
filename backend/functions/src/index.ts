/**
 * Cloud Functions entry point. Every exported symbol here becomes a deployed function —
 * see Part 1, section 9.3.9 ("Cloud Functions / Cloud Run") and section 9.3.14 (DevOps) for
 * how these fit into the wider architecture and CI/CD pipeline.
 */
export { onUserCreate, assignUserRole } from "./auth";
export { checkoutOrder } from "./checkout";
export { onOrderCreate, onOrderStatusChange } from "./orders";
export { proposeSubstitution, respondToSubstitution } from "./substitution";
export { acceptDeliveryRun, confirmProofOfDelivery } from "./delivery";
export { assignCoupon } from "./adminOps";
export { onProductWrite } from "./catalogue";
export { sendOrderConfirmationEmail } from "./notifications";
