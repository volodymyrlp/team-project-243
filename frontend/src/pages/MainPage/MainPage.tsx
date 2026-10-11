import { Link } from "react-router-dom";
import logo from "../../assets/images/logo.webp";

import spain from "../../assets/images/SpainImage.webp";
import italy from "../../assets/images/ItalyImage.webp";
import germany from "../../assets/images/GermanyImage.webp";
import austria from "../../assets/images/AustriaImage.webp";
import france from "../../assets/images/FranceImage.webp";

import airbnbLogo from "../../assets/images/Airbnb.webp";
import bookingLogo from "../../assets/images/booking.svg";
import mastercardLogo from "../../assets/images/Mastercard.svg";
import ryanairLogo from "../../assets/images/Ryanair.svg";
import visaLogo from "../../assets/images/Visa.webp";
import wizzairLogo from "../../assets/images/wizzair.webp";

import reviewer1 from "../../assets/images/1.webp";
import reviewer2 from "../../assets/images/2.webp";
import reviewer3 from "../../assets/images/3.webp";
import reviewer4 from "../../assets/images/4.webp";
import reviewer5 from "../../assets/images/5.webp";

import "./MainPage.scss";
import { Header } from "../../components/Header";
import type { Country } from "../../types/Country";
import { WorldMap } from "../../components/WorldMap/WorldMap";

const countries: Country[] = [
  {
    name: "Spain",
    flag: "🇪🇸",
    nights: "54.1M nights",
    description:
      "Sun-drenched beaches, vibrant culture, and architectural masterpieces.",
    image: spain,
  },
  {
    name: "Italy",
    flag: "🇮🇹",
    nights: "39.1M nights",
    description:
      "Timeless history, romantic streets, and unforgettable cuisine.",
    image: italy,
  },
  {
    name: "Germany",
    flag: "🇩🇪",
    nights: "37.0M nights",
    description:
      "Rich heritage, fairy-tale castles, and dynamic modern cities.",
    image: germany,
  },
  {
    name: "Austria",
    flag: "🇦🇹",
    nights: "30.1M nights",
    description:
      "Majestic alpine landscapes, classical music roots, and imperial palaces.",
    image: austria,
  },
  {
    name: "France",
    flag: "🇫🇷",
    nights: "28.5M nights",
    description:
      "Iconic landmarks, world-class art, and exquisite wine regions.",
    image: france,
  },
];

const reviews = [
  {
    id: 1,
    name: "Kateryna",
    role: "Travel Blogger",
    avatar: reviewer1,
    text: "Planning a trip became so simple when all the interesting places are instantly visible on the map. Definitely bringing this on all my future travels!",
  },
  {
    id: 2,
    name: "Maksym M.",
    role: "Traveler",
    avatar: reviewer2,
    text: "I used to put everything into Excel sheets and print out tons of paper. MriyaTrip helps me keep my travel plans organized in one place.",
  },
  {
    id: 3,
    name: "Anna",
    role: "Travel Enthusiast",
    avatar: reviewer3,
    text: "Having all my destinations in one place makes planning a trip much easier. I can organize my ideas before setting off on an adventure.",
  },
  {
    id: 4,
    name: "Andrii",
    role: "Frequent Traveler",
    avatar: reviewer4,
    text: "I love having a clear overview of the places I want to visit. It makes preparing for a new journey feel much more organized.",
  },
  {
    id: 5,
    name: "Olha",
    role: "Adventure Seeker",
    avatar: reviewer5,
    text: "MriyaTrip gives me a convenient place to collect my travel plans and keep track of the destinations I want to explore.",
  },
];

export const MainPage = () => {
  const token = localStorage.getItem("token");
  const isAuthenticated = Boolean(token);

  return (
    <main className="main-before-auth">
      <section className="main-before-auth__hero">
        <Header />

        <div className="main-before-auth__hero-content">
          <span className="main-before-auth__eyebrow">
            YOUR NEXT ADVENTURE STARTS HERE
          </span>

          <h1>
            Your dream.
            <br />
            Your journey.
            <br />
            Your perfect plan.
          </h1>

          <p>
            Turn your travel dreams into clear routes. Gather locations,
            tickets, and inspiration in one cozy space where planning brings
            just as much joy as the journey itself.
          </p>

          <Link
            to={isAuthenticated ? "/trips/create" : "/registration"}
            className="main-before-auth__cta-button"
          >
            {isAuthenticated ? "Create your trip" : "Start planning"}
          </Link>
        </div>
      </section>

      <section className="main-before-auth__countries">
        <div className="main-before-auth__section-heading">
          <span className="main-before-auth__section-label">
            DESTINATIONS
          </span>

          <h2>Top 5 countries to spark your wanderlust</h2>

          <p>
            Explore the most desired destinations chosen by our travelers this
            season.
          </p>
        </div>

        <div className="main-before-auth__countries-grid">
          {countries.map((country, index) => (
            <article
              className={`country-card ${
                index === 0 ? "country-card--featured" : ""
              }`}
              key={country.name}
            >
              <div className="country-card__image-wrapper">
                <img
                  src={country.image}
                  alt={country.name}
                  className="country-card__image"
                />

                <span className="country-card__rank">#{index + 1}</span>
              </div>

              <div className="country-card__content">
                <h3>
                  <span>{country.flag}</span>
                  {country.name}
                </h3>

                <span className="country-card__nights">
                  {country.nights}
                </span>

                <p>{country.description}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <WorldMap />

      <section className="main-before-auth__how-it-works">
        <div className="main-before-auth__how-heading">
          <span>HOW IT WORKS</span>

          <h2>Start planning your next adventure</h2>

          <p>
            MriyaTrip helps you organize your travels, keep your trips in one
            place, and explore the world.
          </p>
        </div>

        <div className="main-before-auth__steps">
          <article className="main-before-auth__step">
            <div className="main-before-auth__step-icon">👤</div>

            <span className="main-before-auth__step-number">01</span>

            <h3>Create an account</h3>

            <p>
              Sign up and create your personal profile to start planning your
              adventures.
            </p>
          </article>

          <article className="main-before-auth__step">
            <div className="main-before-auth__step-icon">📍</div>

            <span className="main-before-auth__step-number">02</span>

            <h3>Create your trip</h3>

            <p>
              Choose your destination, dates, and add all the important details
              about your trip.
            </p>
          </article>

          <article className="main-before-auth__step">
            <div className="main-before-auth__step-icon">🌍</div>

            <span className="main-before-auth__step-number">03</span>

            <h3>Plan &amp; explore</h3>

            <p>
              Keep your trips organized and see your destinations on your
              personal travel map.
            </p>
          </article>
        </div>

        <Link
          to={isAuthenticated ? "/trips/create" : "/registration"}
          className="main-before-auth__how-button"
        >
          {isAuthenticated ? "Create your trip" : "Create an account"}
        </Link>
      </section>

      <section className="main-before-auth__partners">
        <div className="main-before-auth__section-heading">
          <span className="main-before-auth__section-label">
            TRAVEL RESOURCES
          </span>

          <h2>Everything you need for your next adventure</h2>

          <p>
            Discover services that can help you find accommodation, book
            flights, and prepare for your journey.
          </p>
        </div>

        <div className="partners-grid">
          <a
            className="partner-card"
            href="https://www.airbnb.com/"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Airbnb"
          >
            <img src={airbnbLogo} alt="Airbnb" />
          </a>

          <a
            className="partner-card"
            href="https://www.booking.com/"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Booking.com"
          >
            <img src={bookingLogo} alt="Booking.com" />
          </a>

          <a
            className="partner-card"
            href="https://www.ryanair.com/"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Ryanair"
          >
            <img src={ryanairLogo} alt="Ryanair" />
          </a>

          <a
            className="partner-card"
            href="https://wizzair.com/"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Wizz Air"
          >
            <img src={wizzairLogo} alt="Wizz Air" />
          </a>

          <div className="partner-card">
            <img src={mastercardLogo} alt="Mastercard" />
          </div>

          <a
            className="partner-card"
            href="https://www.visa.com/"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Visit Visa"
          >
            <img src={visaLogo} alt="Visa" />
          </a>
        </div>
      </section>

      <section className="main-before-auth__reviews">
        <div className="main-before-auth__section-heading">
          <span className="main-before-auth__section-label">
            TRAVELERS' STORIES
          </span>

          <h2>What travelers say about MriyaTrip</h2>

          <p>
            Discover how MriyaTrip helps travelers organize their journeys
            and plan their next adventures.
          </p>
        </div>

        <div className="main-before-auth__reviews-grid">
          {reviews.map((review) => (
            <article className="review-card" key={review.id}>
              <div
                className="review-card__stars"
                aria-label="5 out of 5 stars"
              >
                ★★★★★
              </div>

              <blockquote>{review.text}</blockquote>

              <div className="review-card__author">
                <img
                  src={review.avatar}
                  alt={review.name}
                  className="review-card__avatar"
                />

                <div>
                  <strong>{review.name}</strong>
                  <span>{review.role}</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="main-before-auth__final-cta">
        <div className="main-before-auth__final-cta-content">
          <span className="main-before-auth__section-label">
            YOUR JOURNEY AWAITS
          </span>

          <h2>Time to turn dreams into reality</h2>

          <p>
            Create your first itinerary in a few minutes and head out toward new
            emotions.
          </p>

          <Link
            to={isAuthenticated ? "/trips/create" : "/registration"}
            className="main-before-auth__cta-button main-before-auth__cta-button--light"
          >
            Create your first trip
          </Link>
        </div>
      </section>

      <footer className="main-before-auth__footer">
        <div className="main-before-auth__footer-brand">
          <img
            src={logo}
            alt="MriyaTrip"
            className="main-before-auth__footer-logo"
          />

          <span>MriyaTrip</span>
        </div>

        <p>Plan less. Travel more.</p>

        <span className="main-before-auth__copyright">
          © 2026 MriyaTrip. All rights reserved.
        </span>
      </footer>
    </main>
  );
};
