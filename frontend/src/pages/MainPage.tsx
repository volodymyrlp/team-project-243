import { Link } from "react-router-dom";
import logo from "../assets/images/logo.png";

import spain from "../assets/images/SpainImage.jpg";
import italy from "../assets/images/ItalyImage.jpg";
import germany from "../assets/images/GermanyImage.jpg";
import austria from "../assets/images/AustriaImage.jpg";
import france from "../assets/images/FranceImage.jpg";

import "./MainPage.scss";

interface Country {
  name: string;
  flag: string;
  nights: string;
  description: string;
  image: string;
}

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

export const MainPage = () => {
  return (
    <main className='main-before-auth'>

      <section className='main-before-auth__hero'>
        <header className='main-before-auth__header'>
          <Link
            to='/'
            className='main-before-auth__brand'
          >
            <img
              src={logo}
              alt='MriyaTrip'
              className='main-before-auth__logo'
            />

            <span className='main-before-auth__brand-name'>MriyaTrip</span>
          </Link>

          <nav className='main-before-auth__nav'>
            <Link to='/login'>Log in</Link>

            <Link
              to='/registration'
              className='main-before-auth__nav-button'
            >
              Sign up
            </Link>
          </nav>
        </header>

        <div className='main-before-auth__hero-content'>
          <span className='main-before-auth__eyebrow'>
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
            to='/registration'
            className='main-before-auth__cta-button'
          >
            Start planning
          </Link>
        </div>
      </section>


      <section className='main-before-auth__countries'>
        <div className='main-before-auth__section-heading'>
          <span className='main-before-auth__section-label'>DESTINATIONS</span>

          <h2>Top 5 countries to spark your wanderlust</h2>

          <p>
            Explore the most desired destinations chosen by our travelers this
            season.
          </p>
        </div>

        <div className='main-before-auth__countries-grid'>
          {countries.map((country, index) => (
            <article
              className={`country-card ${
                index === 0 ? "country-card--featured" : ""
              }`}
              key={country.name}
            >
              <div className='country-card__image-wrapper'>
                <img
                  src={country.image}
                  alt={country.name}
                  className='country-card__image'
                />

                <span className='country-card__rank'>#{index + 1}</span>
              </div>

              <div className='country-card__content'>
                <h3>
                  <span>{country.flag}</span>
                  {country.name}
                </h3>

                <span className='country-card__nights'>{country.nights}</span>

                <p>{country.description}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className='main-before-auth__reviews'>
        <div className='main-before-auth__section-heading'>
          <span className='main-before-auth__section-label'>
            TRAVELERS' STORIES
          </span>

          <h2>What travelers say about MriyaTrip</h2>

          <p>
            Thousands of people have already tried the service and fallen in
            love with effortless travel planning.
          </p>
        </div>

        <div className='main-before-auth__reviews-grid'>
          <article className='review-card'>
            <div className='review-card__stars'>★★★★★</div>

            <blockquote>
              "Planning a trip became so simple when all the interesting places
              are instantly visible on the map. Definitely bringing this on all
              my future travels!"
            </blockquote>

            <div className='review-card__author'>
              <div className='review-card__avatar'>K</div>

              <div>
                <strong>Kateryna</strong>
                <span>Travel Blogger</span>
              </div>
            </div>
          </article>

          <article className='review-card'>
            <div className='review-card__stars'>★★★★★</div>

            <blockquote>
              "I used to put everything into Excel sheets and print out tons of
              paper. MriyaTrip completely changed my approach: maps, bookings,
              expenses — everything is in one phone."
            </blockquote>

            <div className='review-card__author'>
              <div className='review-card__avatar'>M</div>

              <div>
                <strong>Maksym M.</strong>
                <span>Traveler</span>
              </div>
            </div>
          </article>
        </div>
      </section>


      <section className='main-before-auth__final-cta'>
        <div className='main-before-auth__final-cta-content'>
          <span className='main-before-auth__section-label'>
            YOUR JOURNEY AWAITS
          </span>

          <h2>Time to turn dreams into reality</h2>

          <p>
            Create your first itinerary in a few minutes and head out toward new
            emotions.
          </p>

          <Link
            to='/registration'
            className='main-before-auth__cta-button main-before-auth__cta-button--light'
          >
            Create your first trip
          </Link>
        </div>
      </section>

      <footer className='main-before-auth__footer'>
        <div className='main-before-auth__footer-brand'>
          <img
            src={logo}
            alt='MriyaTrip'
            className='main-before-auth__footer-logo'
          />

          <span>MriyaTrip</span>
        </div>

        <p>Plan less. Travel more.</p>

        <span className='main-before-auth__copyright'>
          © 2026 MriyaTrip. All rights reserved.
        </span>
      </footer>
    </main>
  );
};
