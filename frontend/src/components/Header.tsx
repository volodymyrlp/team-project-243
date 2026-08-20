import { Link, useNavigate } from "react-router-dom";
import logo from "../assets/images/logo.png";
import "./Header.scss";

export const Header = () => {
  const navigate = useNavigate();

  const token = localStorage.getItem("token");
  const isAuthenticated = Boolean(token);

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <header className='header'>
      <Link
        to='/'
        className='header__brand'
      >
        <img
          src={logo}
          alt='MriyaTrip'
          className='header__logo'
        />

        <span className='header__brand-name'>MriyaTrip</span>
      </Link>

      <nav className='header__nav'>
        {isAuthenticated ? (
          <>
            <Link
              to='/profile'
              className='header__profile'
            >
              Profile
            </Link>

            <button
              type='button'
              className='header__logout'
              onClick={handleLogout}
            >
              Log out
            </button>
          </>
        ) : (
          <>
            <Link to='/login'>Log in</Link>

            <Link
              to='/registration'
              className='header__nav-button'
            >
              Sign up
            </Link>
          </>
        )}
      </nav>
    </header>
  );
};
