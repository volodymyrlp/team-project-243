import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RegistrationPage } from "./pages/RegistrationPage";
import { LoginPage } from "./pages/LoginPage";
import "./App.scss";
import { MainPage } from "./pages/MainPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path='/registration'
          element={<RegistrationPage />}
        />

        <Route
          path='/login'
          element={<LoginPage />}
        />

        <Route
          path='*'
          element={<LoginPage />}
        />
        <Route
          path='/'
          element={<MainPage />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
