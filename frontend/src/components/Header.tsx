import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import logo from "../assets/images/logo.png";
import "./Header.scss";

export const Header = () => {
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const token = localStorage.getItem("token");
  const isAuthenticated = Boolean(token);

  const closeMenu = () => {
    setIsMenuOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    closeMenu();
    navigate("/");
  };

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeMenu();
      }
    };

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMenuOpen]);

  return (
    <header className="header">
      <Link
        to="/"
        className="header__brand"
        onClick={closeMenu}
      >
        <img
          src={logo}
          alt="MriyaTrip"
          className="header__logo"
        />

        <span className="header__brand-name">MriyaTrip</span>
      </Link>

      <button
        type="button"
        className={`header__menu-toggle ${
          isMenuOpen ? "header__menu-toggle--open" : ""
        }`}
        onClick={() => setIsMenuOpen((prev) => !prev)}
        aria-label={isMenuOpen ? "Close navigation menu" : "Open navigation menu"}
        aria-expanded={isMenuOpen}
        aria-controls="header-navigation"
      >
        <span />
        <span />
        <span />
      </button>

      <nav
        id="header-navigation"
        className={`header__nav ${
          isMenuOpen ? "header__nav--open" : ""
        }`}
        aria-label="Main navigation"
      >
        {isAuthenticated ? (
          <>
            <Link
              to="/trips/create"
              className="header__create-trip"
              onClick={closeMenu}
            >
              Create trip
            </Link>

            <Link
              to="/profile"
              className="header__profile"
              onClick={closeMenu}
            >
              Profile
            </Link>

            <button
              type="button"
              className="header__logout"
              onClick={handleLogout}
            >
              Log out
            </button>
          </>
        ) : (
          <>
            <Link
              to="/login"
              onClick={closeMenu}
            >
              Log in
            </Link>

            <Link
              to="/registration"
              className="header__nav-button"
              onClick={closeMenu}
            >
              Sign up
            </Link>
          </>
        )}
      </nav>
    </header>
  );
};
