import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User
} from "firebase/auth";
import { auth } from "./firebase";

type AuthState = {
  user: User | null;
  role: string | null;
  loading: boolean;
  error: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | undefined>(undefined);

/**
 * Reads the `role` custom claim set on the user's ID token (see functions/src/auth.ts
 * assignUserRole, or the seed scripts' direct auth.setCustomUserClaims calls) — the same
 * mechanism the Android app and the Firestore/RTDB security rules use for RBAC. A customer,
 * picker, or driver account can sign in here but never sees anything, because every read this
 * app makes is itself gated by those same security rules requiring role in [ADMIN, MANAGER].
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    return onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        const tokenResult = await firebaseUser.getIdTokenResult(true);
        setRole((tokenResult.claims.role as string | undefined) ?? null);
      } else {
        setRole(null);
      }
      setLoading(false);
    });
  }, []);

  const signIn = async (email: string, password: string) => {
    setError(null);
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Sign-in failed");
      throw err;
    }
  };

  const signOut = async () => {
    await firebaseSignOut(auth);
  };

  return (
    <AuthContext.Provider value={{ user, role, loading, error, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}
