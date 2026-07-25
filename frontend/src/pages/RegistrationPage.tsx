import { useState } from "react";
import google from "./google.svg";
import "./RegistrationPage.scss";
import { PhoneInput } from "react-international-phone";
import "react-international-phone/style.css";

export const RegistrationPage = () => {
  const [phone, setPhone] = useState("");

  return (
    <section className="page login-9">
      <div className="login-9-card">
        <div className="login-9-hero">
          <div className="ocean">
            <div className="wave"></div>
            <div className="wave"></div>
          </div>
        </div>

        <form className="login-9-form">
          <h3>Create your account</h3>

          <button
            type="button"
            className="login-9-social-btn"
          >
            <img
              src={google}
              alt="Google"
            />

            <span>
              <span className="login-9-extra-text">
                Sign up with{" "}
              </span>
              Google
            </span>
          </button>


          <span className="login-9-or"></span>


          <input
            type="text"
            placeholder="Full name"
          />

          <input
            type="email"
            placeholder="Email"
          />


          <PhoneInput
            className="phone-input"
            defaultCountry="ua"
            value={phone}
            onChange={setPhone}
          />


          <input
            type="password"
            placeholder="Password"
          />

          <input
            type="password"
            placeholder="Confirm password"
          />


          <button type="submit">
            Sign up
          </button>
        </form>
      </div>
    </section>
  );
};