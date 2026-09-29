import * as functions from "firebase-functions";
import { Product } from "./types";

/**
 * Maintains a lowercase `nameSearchKey` field so FirestoreRepository.searchProducts (Android)
 * can do a cheap prefix-range query (orderBy + startAt/endAt) without standing up a dedicated
 * search service — documented as a deliberate scope trade-off in FirestoreRepository.kt.
 */
export const onProductWrite = functions.firestore
  .document("storeNodes/{storeId}/products/{productId}")
  .onWrite(async (change) => {
    if (!change.after.exists) {
      return;
    }
    const product = change.after.data() as Product;
    const expectedKey = product.name.toLowerCase();
    if (product.nameSearchKey !== expectedKey) {
      await change.after.ref.update({ nameSearchKey: expectedKey });
    }
  });
