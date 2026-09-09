import { BrowserRouter, Routes, Route } from "react-router-dom";

import { RegistrationPage } from "./pages/RegistrationPage/RegistrationPage";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { MainPage } from "./pages/MainPage/MainPage";
import { ProfilePage } from "./pages/ProfilePage/ProfilePage";

import { ProtectedRoute } from "./components/ProtectedRoute";

import "./App.scss";
import { CreateTripPage } from "./pages/CreateTripPage/CreateTripPage";

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

        <Route
          path='/create-trip'
          element={<CreateTripPage />} // ЗАБРАТИ!!!!
        />

        {/* Protected routes */}

        <Route element={<ProtectedRoute />}>
          <Route
            path='/profile'
            element={<ProfilePage />}
          />
          <Route
            path='/trips/create'
            element={<CreateTripPage />}
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
