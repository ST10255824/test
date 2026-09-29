import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getDatabase } from "firebase/database";

/**
 * Same Firebase project as the Android app (see app/google-services.json) — this console reads
 * and writes the exact same Firestore/Realtime Database data the customer/picker/driver app
 * uses, enforced by the same Firestore Security Rules (firestore/firestore.rules), which is
 * why this file can only ever act as an ADMIN or MANAGER: everything else the rules reject.
 *
 * The apiKey below is not a secret — Firebase web API keys identify the project, they don't
 * authorise anything by themselves. Actual access control lives in firestore/firestore.rules
 * and database.rules.json, not in hiding this value.
 */
export const firebaseConfig = {
  projectId: "mackson-delivery",
  appId: "1:245519242971:web:328aa31a4547796b5b5efe",
  databaseURL: "https://mackson-delivery-default-rtdb.europe-west1.firebasedatabase.app",
  storageBucket: "mackson-delivery.firebasestorage.app",
  apiKey: "AIzaSyAmVclpjLhsg3PFt64ebu6N6KoTQdQhL-4",
  authDomain: "mackson-delivery.firebaseapp.com",
  messagingSenderId: "245519242971"
};

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const db = getFirestore(firebaseApp);
export const rtdb = getDatabase(firebaseApp);

export const STORE_ID = "store-estcourt";
