import { useState } from "react";
import "./LoginPage.scss";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../api/auth";

interface LoginForm {
  email: string;
  password: string;
}

export const LoginPage = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState<LoginForm>({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showErrorModal, setShowErrorModal] = useState(false);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [event.target.name]: event.target.value,
    });

    setError("");
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    setError("");

    try {
      setLoading(true);

      const response = await loginUser(formData);

      localStorage.setItem("token", response.token);

      navigate("/");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Something went wrong. Please try again.";

      setError(message);
      setShowErrorModal(true);
    } finally {
      setLoading(false);
    }
  };

  const handleCloseModal = () => {
    setShowErrorModal(false);
    setError("");
  };

  return (
    <section className='page login'>
      <div className='login-card'>
        <div className='login-hero'>
          <div className='login-hero-content'>
            <h2>Welcome back</h2>

            <p>Continue your journey and explore new destinations.</p>
          </div>

          <div className='ocean'>
            <div className='wave' />
            <div className='wave' />
          </div>
        </div>

        <form
          className='login-form'
          onSubmit={handleSubmit}
        >
          <Link
            to='/'
            className='login-form__back'
          >
            ← Back to home
          </Link>
          <h3>Login to your account</h3>

          <input
            name='email'
            type='email'
            placeholder='Email'
            value={formData.email}
            onChange={handleChange}
            autoComplete='email'
            required
          />

          <input
            name='password'
            type='password'
            placeholder='Password'
            value={formData.password}
            onChange={handleChange}
            autoComplete='current-password'
            required
          />

          <button
            type='submit'
            disabled={loading}
          >
            {loading ? "Logging in..." : "Log in"}
          </button>

          <p>
            Don't have an account?{" "}
            <Link
              className='form-link'
              to='/registration'
            >
              Sign up
            </Link>
          </p>
        </form>
      </div>

      {showErrorModal && (
        <div
          className='login-modal'
          onMouseDown={handleCloseModal}
        >
          <div
            className='login-modal__content'
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className='login-modal__icon'>!</div>

            <h2>Login failed</h2>

            <p>
              {error ||
                "We couldn't log you in. Please check your email and password."}
            </p>

            <button
              type='button'
              className='login-modal__button'
              onClick={handleCloseModal}
            >
              Try again
            </button>
          </div>
        </div>
      )}
    </section>
  );
};
