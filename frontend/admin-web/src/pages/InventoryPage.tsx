import { useEffect, useMemo, useState } from "react";
import { collection, doc, onSnapshot, updateDoc } from "firebase/firestore";
import { db, STORE_ID } from "../lib/firebase";
import type { Product } from "../lib/types";

/** Live inventory override — Part 1 US-23. Writes straight to Firestore (permitted for
 * ADMIN/MANAGER by firestore/firestore.rules), same as the Android AdminInventoryScreen. */
export function InventoryPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState<string>("ALL");
  const [search, setSearch] = useState("");

  useEffect(() => {
    const productsRef = collection(db, "storeNodes", STORE_ID, "products");
    return onSnapshot(productsRef, (snapshot) => {
      setProducts(snapshot.docs.map((d) => ({ productId: d.id, ...d.data() }) as Product));
      setLoading(false);
    });
  }, []);

  const categories = useMemo(() => Array.from(new Set(products.map((p) => p.category))).sort(), [products]);

  const visible = products
    .filter((p) => category === "ALL" || p.category === category)
    .filter((p) => p.name.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => a.name.localeCompare(b.name));

  const adjustStock = async (product: Product, delta: number) => {
    const newLevel = Math.max(0, product.currentStockLevel + delta);
    await updateDoc(doc(db, "storeNodes", STORE_ID, "products", product.productId), {
      currentStockLevel: newLevel
    });
  };

  return (
    <div>
      <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Inventory</h1>
      <p style={{ color: "var(--text-secondary)", margin: "0 0 20px" }}>
        {loading ? "Loading…" : `${products.length} products at ${STORE_ID}`}. Changes apply instantly to the
        customer app's catalogue.
      </p>

      <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
        <input placeholder="Search products…" value={search} onChange={(e) => setSearch(e.target.value)} style={{ flex: 1 }} />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="ALL">All categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      <div className="card">
        {loading ? (
          <p style={{ color: "var(--text-secondary)" }}>Loading products…</p>
        ) : (
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>Category</th>
              <th>Barcode</th>
              <th>Price</th>
              <th>Stock</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {visible.map((product) => (
              <tr key={product.productId}>
                <td style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <img
                    src={product.imageUrl}
                    alt=""
                    style={{ width: 36, height: 36, borderRadius: 8, objectFit: "cover", background: "var(--surface-tint)" }}
                  />
                  {product.name}
                </td>
                <td>{product.category}</td>
                <td>{product.barcode}</td>
                <td>R{product.unitPrice.toFixed(2)}</td>
                <td>
                  <span
                    style={{
                      fontWeight: 700,
                      color: product.currentStockLevel === 0 ? "var(--error)" : "var(--text-primary)"
                    }}
                  >
                    {product.currentStockLevel}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: 6 }}>
                    <button className="pill-button outline" style={{ padding: "4px 12px" }} onClick={() => adjustStock(product, -1)}>
                      −
                    </button>
                    <button className="pill-button outline" style={{ padding: "4px 12px" }} onClick={() => adjustStock(product, 1)}>
                      +
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        )}
      </div>
    </div>
  );
}
