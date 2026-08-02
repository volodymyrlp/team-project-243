import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RegistrationPage } from "./pages/RegistrationPage";
import { LoginPage } from "./pages/LoginPage";
import "./App.scss";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/registration"
          element={<RegistrationPage />}
        />

        <Route
          path="/login"
          element={<LoginPage />}
        />

        <Route
          path="*"
          element={<LoginPage />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;