import { BrowserRouter, Routes, Route } from "react-router-dom";

import ScrollToTop from "./components/ScrollToTop";
import { RegistrationPage } from "./pages/RegistrationPage/RegistrationPage";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { MainPage } from "./pages/MainPage/MainPage";
import { ProfilePage } from "./pages/ProfilePage/ProfilePage";

import { ProtectedRoute } from "./components/ProtectedRoute";

import "./App.scss";
import { CreateTripPage } from "./pages/CreateTripPage/CreateTripPage";
import { ItineraryBuilderPage } from "./pages/ItineraryBuilderPage/ItineraryBuilderPage";
import TravelInspirationPage from "./pages/TravelInspirationPage/TravelInspirationPage";

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />

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
          path='/inspiration'
          element={<TravelInspirationPage />}
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
          <Route
            path='/trips/:tripId/itinerary'
            element={<ItineraryBuilderPage />}
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
