import { useState } from "react";
import "./RegistrationPage.scss";
import { registerUser } from "../api/auth";
import { Link, useNavigate } from "react-router-dom";

interface RegistrationForm {
  fullName: string;
  email: string;
  passwordHash: string;
  confirmPassword: string;
}

type ModalType = "success" | "error" | null;

export const RegistrationPage = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState<RegistrationForm>({
    fullName: "",
    email: "",
    passwordHash: "",
    confirmPassword: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState<ModalType>(null);

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

    if (formData.passwordHash.length < 8 || formData.passwordHash.length > 20) {
      setError("Password must be 8-20 characters.");

      return;
    }

    if (formData.passwordHash !== formData.confirmPassword) {
      setError("Passwords do not match.");

      return;
    }

    try {
      setLoading(true);

      await registerUser(formData);

      setFormData({
        fullName: "",
        email: "",
        passwordHash: "",
        confirmPassword: "",
      });

      setModal("success");
    } catch (error) {
      setModal("error");

      setError(
        error instanceof Error
          ? error.message
          : "Something went wrong. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCloseModal = () => {
    setModal(null);
    setError("");
  };

  const handleLogin = () => {
    setModal(null);
    navigate("/login");
  };

  return (
    <section className='page registration'>
      <div className='registration-card'>
        <div className='registration-hero'>
          <div className='registration-hero-content'>
            <h2>Explore the world</h2>

            <p>Plan your trips easily and create unforgettable memories.</p>
          </div>

          <div className='ocean'>
            <div className='wave' />
            <div className='wave' />
          </div>
        </div>

        <form
          className='registration-form'
          onSubmit={handleSubmit}
        >
          <Link
            to='/'
            className='registration-form__back'
          >
            ← Back to home
          </Link>
          <h3>Create your account</h3>

          <input
            name='fullName'
            type='text'
            placeholder='Full name'
            value={formData.fullName}
            onChange={handleChange}
            required
          />

          <input
            name='email'
            type='email'
            placeholder='Email'
            value={formData.email}
            onChange={handleChange}
            required
          />

          <input
            name='passwordHash'
            type='password'
            placeholder='Password'
            value={formData.passwordHash}
            onChange={handleChange}
            required
          />

          <input
            name='confirmPassword'
            type='password'
            placeholder='Confirm password'
            value={formData.confirmPassword}
            onChange={handleChange}
            required
          />

          {error && <p className='error-message'>{error}</p>}

          <button
            type='submit'
            disabled={loading}
          >
            {loading ? "Creating..." : "Sign up"}
          </button>

          <p>
            Already have an account?{" "}
            <Link
              className='form-link'
              to='/login'
            >
              Login
            </Link>
          </p>
        </form>
      </div>

      {modal && (
        <div
          className='registration-modal'
          onMouseDown={handleCloseModal}
        >
          <div
            className='registration-modal__content'
            onMouseDown={(event) => event.stopPropagation()}
          >
            {modal === "success" ? (
              <>
                <div className='registration-modal__icon registration-modal__icon--success'>
                  ✓
                </div>

                <h2>Account created!</h2>

                <p>
                  Your account has been created successfully. You can now log in
                  and start planning your trips.
                </p>

                <button
                  type='button'
                  className='registration-modal__button'
                  onClick={handleLogin}
                >
                  Log in
                </button>
              </>
            ) : (
              <>
                <div className='registration-modal__icon registration-modal__icon--error'>
                  !
                </div>

                <h2>Registration failed</h2>

                <p>
                  {error ||
                    "We couldn't create your account. Please try again."}
                </p>

                <button
                  type='button'
                  className='registration-modal__button'
                  onClick={handleCloseModal}
                >
                  Try again
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </section>
  );
};
