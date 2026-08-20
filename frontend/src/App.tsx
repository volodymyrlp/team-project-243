import { BrowserRouter, Routes, Route } from "react-router-dom";

import { RegistrationPage } from "./pages/RegistrationPage";
import { LoginPage } from "./pages/LoginPage";
import { MainPage } from "./pages/MainPage";
import { ProfilePage } from "./pages/ProfilePage";

import { ProtectedRoute } from "./components/ProtectedRoute";

import "./App.scss";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path='/'
          element={<MainPage />}
        />
        <Route
          path='/login'
          element={<LoginPage />}
        />
        <Route
          path='/registration'
          element={<RegistrationPage />}
        />

        {/* Protected routes */}

        <Route element={<ProtectedRoute />}>
          <Route
            path='/profile'
            element={<ProfilePage />}
          />
        </Route>
        <Route
          path='*'
          element={<MainPage />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
