import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./lib/AuthContext";
import { RequireAdmin } from "./components/RequireAdmin";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { OrdersPage } from "./pages/OrdersPage";
import { InventoryPage } from "./pages/InventoryPage";
import { CustomersPage } from "./pages/CustomersPage";
import { PickersPage } from "./pages/PickersPage";
import { DriversPage } from "./pages/DriversPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <RequireAdmin>
                <Layout />
              </RequireAdmin>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/customers" element={<CustomersPage />} />
            <Route path="/pickers" element={<PickersPage />} />
            <Route path="/drivers" element={<DriversPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
