import { useState } from "react";
import google from "../assets/images/google.svg";
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

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [event.target.name]: event.target.value,
    });
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
      setError(error instanceof Error ? error.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
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
          <h3>Login to your account</h3>

          <button
            type='button'
            className='login-social-btn'
          >
            <img
              src={google}
              alt='Google'
            />
            Continue with Google
          </button>

          <span className='login-or' />

          <input
            name='email'
            type='email'
            placeholder='Email'
            value={formData.email}
            onChange={handleChange}
            required
          />

          <input
            name='password'
            type='password'
            placeholder='Password'
            value={formData.password}
            onChange={handleChange}
            required
          />

          {error && <p className='error-message'>{error}</p>}

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
    </section>
  );
};
