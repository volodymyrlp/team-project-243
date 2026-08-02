import { useState } from "react";
import google from "../assets/images/google.svg";
import "./RegistrationPage.scss";
import { registerUser } from "../api/auth";

interface RegistrationForm {
  fullName: string;
  email: string;
  passwordHash: string;
  confirmPassword: string;
}

export const RegistrationPage = () => {
  const [formData, setFormData] = useState<RegistrationForm>({
    fullName: "",
    email: "",
    passwordHash: "",
    confirmPassword: "",
  });

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
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
    setSuccess("");

    if (formData.passwordHash.length < 8 || formData.passwordHash.length > 20) {
      setError("Password must be 8-20 characters");

      return;
    }

    if (formData.passwordHash !== formData.confirmPassword) {
      setError("Passwords do not match");

      return;
    }

    try {
      setLoading(true);

      await registerUser(formData);

      setSuccess("Account created successfully!");

      setFormData({
        fullName: "",
        email: "",
        passwordHash: "",
        confirmPassword: "",
      });
    } catch (error) {
      setError(error instanceof Error ? error.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
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
          <h3>Create your account</h3>

          <button
            type='button'
            className='registration-social-btn'
          >
            <img
              src={google}
              alt='Google'
            />
            Sign up with Google
          </button>

          <span className='registration-or' />

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

          {success && <p className='success-message'>{success}</p>}

          <button
            type='submit'
            disabled={loading}
          >
            {loading ? "Creating..." : "Sign up"}
          </button>
        </form>
      </div>
    </section>
  );
};
