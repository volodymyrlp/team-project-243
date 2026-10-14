import { useState } from "react";
import { useNavigate } from "react-router-dom";

import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

import "../../styles/DatePicker.scss";
import "./TravelInspirationPage.scss";

import RomeImage from "../../assets/images/Rome.webp";
import LvivImage from "../../assets/images/Lviv.webp";
import ParisImage from "../../assets/images/Paris.webp";

import { Header } from "../../components/Header";

interface InspirationDay {
  title: string;
  description: string;
  activities: string[];
}

interface InspirationTrip {
  id: string;
  title: string;
  subtitle: string;
  destination: string;
  duration: number;
  image: string;
  description: string;
  tags: string[];
  days: InspirationDay[];
}

interface CreatedTripResponse {
  tripId: string;
}

interface PresignResponse {
  uploadUrl: string;
  objectKey: string;
}

const MAX_COVER_SIZE = 5 * 1024 * 1024;

const inspirationTrips: InspirationTrip[] = [
  {
    id: "lviv-autumn",
    title: "Cultural Autumn in Lviv",
    subtitle: "4 Days of Inspiration, Coffee, and Art",
    destination: "Lviv, Ukraine",
    duration: 4,
    image: LvivImage,
    description:
      "Discover Lviv's intellectual and artistic soul. Enjoy crisp mornings with coffee on a terrace, explore contemporary art, and spend an elegant evening at the Opera.",
    tags: ["Culture", "Art", "Coffee"],
    days: [
      {
        title: "Autumn Atmosphere",
        description:
          "Ease into the rhythm of Lviv and discover the charm of its historic streets.",
        activities: [
          "Enjoy breakfast and coffee on a cozy terrace.",
          "Explore Rynok Square and the historic city center.",
          "Discover local architecture and hidden streets.",
        ],
      },
      {
        title: "Artistic Immersion",
        description:
          "Experience the city's creative side through contemporary art and local galleries.",
        activities: [
          "Visit a contemporary art exhibition.",
          "Explore independent galleries and creative spaces.",
          "Finish the day at a relaxed local café.",
        ],
      },
      {
        title: "A Night in High Style",
        description:
          "Enjoy an elegant evening surrounded by architecture, music, and autumn colors.",
        activities: [
          "Explore the city's architectural landmarks.",
          "Attend a performance at the Lviv National Opera, subject to the schedule.",
          "Take an evening walk through the illuminated streets.",
        ],
      },
      {
        title: "Coffee Inspiration",
        description:
          "Slow down, discover local favorites, and collect a few final memories.",
        activities: [
          "Visit a specialty coffee shop.",
          "Browse concept stores and local design shops.",
          "Enjoy one last walk before departure.",
        ],
      },
    ],
  },
  {
    id: "rome-weekend",
    title: "All Roads Lead to Rome",
    subtitle: "The Ultimate 3-Day Weekend in the Eternal City",
    destination: "Rome, Italy",
    duration: 3,
    image: RomeImage,
    description:
      "Experience the Dolce Vita in three unforgettable days. Discover ancient landmarks, artistic treasures, Italian coffee, and the quieter corners that make Rome special.",
    tags: ["History", "Food", "Architecture"],
    days: [
      {
        title: "Imperial Start",
        description:
          "Travel back in time among the landmarks of Ancient Rome, then discover its lively neighborhoods.",
        activities: [
          "Explore the Colosseum and the Roman Forum.",
          "Walk through the historic center and admire the city's architecture.",
          "Spend the evening discovering the atmosphere of Trastevere.",
        ],
      },
      {
        title: "Art, Coffee & Religious Wonders",
        description:
          "Discover remarkable art, grand architecture, and the rituals of Roman coffee culture.",
        activities: [
          "Explore Vatican City and St. Peter's Basilica.",
          "Visit the Vatican Museums if you have a reservation.",
          "Enjoy Italian coffee and walk through the historic center.",
          "See Piazza Navona and the Trevi Fountain.",
        ],
      },
      {
        title: "The Soul of Rome",
        description:
          "Finish your weekend with local flavors, market discoveries, and panoramic views.",
        activities: [
          "Explore a local market and try traditional Roman food.",
          "Discover a quieter neighborhood away from the busiest tourist streets.",
          "Enjoy a panoramic view of the city before departure.",
        ],
      },
    ],
  },
  {
    id: "paris-bachelorette",
    title: "Bachelorette in Paris",
    subtitle: "2 Days of French Chic, Coffee, and Selfies",
    destination: "Paris, France",
    duration: 2,
    image: ParisImage,
    description:
      "A stylish getaway with your favorite people. Enjoy French cafés, fresh croissants, iconic views, a little shopping, and memorable moments in the City of Light.",
    tags: ["Friends", "Fashion", "City Break"],
    days: [
      {
        title: "French Chic and Elegance",
        description:
          "Start your Paris escape slowly, with great coffee, iconic sights, and beautiful places for photos.",
        activities: [
          "Begin with coffee and fresh croissants at a Parisian café.",
          "Explore the Eiffel Tower area and nearby streets.",
          "Take photos together at some of Paris's iconic locations.",
          "Enjoy a relaxed dinner with your friends.",
        ],
      },
      {
        title: "Shopping, Selfies, and a Glamorous Finale",
        description:
          "Make the most of your final day with Parisian shopping, a scenic stroll, and a special celebration.",
        activities: [
          "Explore boutiques and department stores.",
          "Take a walk along the Seine.",
          "Capture your favorite group photos.",
          "Finish with a memorable bachelorette celebration.",
        ],
      },
    ],
  },
];

function getLocalDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function getDateFromString(value: string): Date | null {
  if (!value) {
    return null;
  }

  const date = new Date(`${value}T12:00:00`);

  return Number.isNaN(date.getTime()) ? null : date;
}

function getEndDate(startDate: string, duration: number): string {
  const date = new Date(`${startDate}T12:00:00`);
  date.setDate(date.getDate() + duration - 1);

  return getLocalDateString(date);
}

async function getErrorMessage(response: Response): Promise<string> {
  const defaultMessage = "Could not add this trip. Please try again.";

  try {
    const data: unknown = await response.json();

    if (typeof data !== "object" || data === null) {
      return defaultMessage;
    }

    const errorData = data as {
      errors?: unknown;
      message?: unknown;
      error?: unknown;
    };

    if (Array.isArray(errorData.errors) && errorData.errors.length > 0) {
      const messages = errorData.errors.filter(
        (error): error is string => typeof error === "string",
      );

      if (messages.length > 0) {
        return messages.join(" ");
      }
    }

    if (typeof errorData.message === "string") {
      return errorData.message;
    }

    if (typeof errorData.error === "string") {
      return errorData.error;
    }
  } catch {
    // The response may not contain JSON.
  }

  return defaultMessage;
}

async function uploadCoverImage(
  imageUrl: string,
  apiUrl: string,
  token: string,
): Promise<string> {
  const imageResponse = await fetch(imageUrl);

  if (!imageResponse.ok) {
    throw new Error("Could not load the trip cover image.");
  }

  const imageBlob = await imageResponse.blob();

  if (imageBlob.size === 0) {
    throw new Error("The trip cover image is empty.");
  }

  if (imageBlob.size > MAX_COVER_SIZE) {
    throw new Error("The trip cover image must be 5 MB or smaller.");
  }

  const contentType = imageBlob.type || "image/webp";

  const presignResponse = await fetch(`${apiUrl}/api/v1/uploads/presign`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      contentType,
      fileSize: imageBlob.size,
    }),
  });

  if (!presignResponse.ok) {
    if (presignResponse.status === 401 || presignResponse.status === 403) {
      throw new Error("Your session may have expired. Please sign in again.");
    }

    throw new Error(await getErrorMessage(presignResponse));
  }

  const presignData: PresignResponse = await presignResponse.json();

  if (!presignData.uploadUrl || !presignData.objectKey) {
    throw new Error("The server returned invalid upload information.");
  }

  const uploadResponse = await fetch(presignData.uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": contentType,
    },
    body: imageBlob,
  });

  if (!uploadResponse.ok) {
    throw new Error(
      `Could not upload the cover image. Status: ${uploadResponse.status}`,
    );
  }

  // 4. Return the storage key, not the temporary upload URL.
  return presignData.objectKey;
}

export default function TravelInspirationPage() {
  const navigate = useNavigate();

  const [selectedTrip, setSelectedTrip] = useState<InspirationTrip | null>(
    null,
  );

  const [startDate, setStartDate] = useState(() =>
    getLocalDateString(new Date()),
  );

  const [isAddingTrip, setIsAddingTrip] = useState(false);
  const [addTripError, setAddTripError] = useState("");
  const [addTripMessage, setAddTripMessage] = useState("");
  const [createdTripId, setCreatedTripId] = useState<string | null>(null);

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const closeDetails = () => {
    if (isAddingTrip) return;

    setSelectedTrip(null);
    setAddTripError("");
    setAddTripMessage("");
    setCreatedTripId(null);
  };

  const openTripDetails = (trip: InspirationTrip) => {
    setSelectedTrip(trip);
    setStartDate(getLocalDateString(new Date()));
    setAddTripError("");
    setAddTripMessage("");
    setCreatedTripId(null);
  };

  const handleAddTrip = async () => {
    if (!selectedTrip || isAddingTrip || createdTripId) return;

    const token = localStorage.getItem("token");

    if (!token) {
      navigate("/login");
      return;
    }

    if (!startDate) {
      setAddTripError("Please select a start date.");
      return;
    }

    const selectedDate = getDateFromString(startDate);

    if (!selectedDate || selectedDate < today) {
      setAddTripError("The start date cannot be in the past.");
      return;
    }

    const apiUrl = import.meta.env.VITE_API_URL?.replace(/\/+$/, "");

    if (!apiUrl) {
      setAddTripError("The API URL is not configured.");
      return;
    }

    setIsAddingTrip(true);
    setAddTripError("");
    setAddTripMessage("");

    try {
      const coverKey = await uploadCoverImage(
        selectedTrip.image,
        apiUrl,
        token,
      );

      const response = await fetch(`${apiUrl}/api/v1/trips`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          title: selectedTrip.title,
          destination: selectedTrip.destination,
          startDate,
          endDate: getEndDate(startDate, selectedTrip.duration),
          description: selectedTrip.description,
          isPublic: false,
          coverUrl: coverKey,
        }),
      });

      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        navigate("/login");

        throw new Error("Your session may have expired. Please sign in again.");
      }

      if (!response.ok) {
        throw new Error(await getErrorMessage(response));
      }

      const createdTrip: CreatedTripResponse = await response.json();

      if (!createdTrip.tripId) {
        throw new Error(
          "The server did not return a trip ID. Please check your trips before trying again.",
        );
      }

      setCreatedTripId(createdTrip.tripId);
      setAddTripMessage(
        "Trip added successfully! You can now find it in your profile.",
      );
    } catch (error) {
      setAddTripError(
        error instanceof Error
          ? error.message
          : "Something went wrong while adding the trip.",
      );
    } finally {
      setIsAddingTrip(false);
    }
  };

  return (
    <>
      <Header />

      <main className='travel-inspiration'>
        <section className='travel-inspiration__hero'>
          <div className='travel-inspiration__hero-content'>
            <span className='travel-inspiration__eyebrow'>
              YOUR NEXT ADVENTURE STARTS HERE
            </span>

            <h1>Find Inspiration for Your Next Adventure</h1>

            <p>
              Discover thoughtfully curated trips, explore new places, and find
              an idea for your next unforgettable journey.
            </p>

            <a
              className='travel-inspiration__hero-button'
              href='#inspiration-trips'
            >
              Explore trips
              <span aria-hidden='true'>→</span>
            </a>
          </div>
        </section>

        <section
          className='travel-inspiration__trips'
          id='inspiration-trips'
        >
          <div className='travel-inspiration__section-heading'>
            <div>
              <span className='travel-inspiration__eyebrow'>
                HANDPICKED FOR YOU
              </span>

              <h2>Trips worth dreaming about</h2>

              <p>
                From cultural city breaks to stylish weekends with friends, find
                the journey that feels right for you.
              </p>
            </div>

            <span className='travel-inspiration__trip-count'>
              {inspirationTrips.length} curated trips
            </span>
          </div>

          <div className='travel-inspiration__grid'>
            {inspirationTrips.map((trip, index) => (
              <article
                className='inspiration-card'
                key={trip.id}
              >
                <div className='inspiration-card__image-wrapper'>
                  <img
                    className='inspiration-card__image'
                    src={trip.image}
                    alt={trip.destination}
                    loading='lazy'
                  />

                  <span className='inspiration-card__number'>0{index + 1}</span>

                  <span className='inspiration-card__duration'>
                    {trip.duration} {trip.duration === 1 ? "day" : "days"}
                  </span>
                </div>

                <div className='inspiration-card__content'>
                  <span className='inspiration-card__destination'>
                    <span aria-hidden='true'>⌖</span>
                    {trip.destination}
                  </span>

                  <h3>{trip.title}</h3>

                  <p className='inspiration-card__subtitle'>{trip.subtitle}</p>

                  <p className='inspiration-card__description'>
                    {trip.description}
                  </p>

                  <div className='inspiration-card__tags'>
                    {trip.tags.map((tag) => (
                      <span
                        className='inspiration-card__tag'
                        key={tag}
                      >
                        {tag}
                      </span>
                    ))}
                  </div>

                  <button
                    className='inspiration-card__button'
                    type='button'
                    onClick={() => openTripDetails(trip)}
                  >
                    Explore trip
                    <span aria-hidden='true'>→</span>
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className='travel-inspiration__cta'>
          <div>
            <span className='travel-inspiration__eyebrow'>
              MAKE IT YOUR OWN
            </span>

            <h2>Your next story is waiting to happen.</h2>

            <p>
              Find an idea you love and start planning a trip that is uniquely
              yours.
            </p>
          </div>

          <a
            className='travel-inspiration__cta-button'
            href='/trips/create'
          >
            Plan your own trip
            <span aria-hidden='true'>→</span>
          </a>
        </section>

        {selectedTrip && (
          <div
            className='trip-details'
            role='presentation'
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) {
                closeDetails();
              }
            }}
          >
            <section
              className='trip-details__dialog'
              role='dialog'
              aria-modal='true'
              aria-labelledby='trip-details-title'
            >
              <button
                className='trip-details__close'
                type='button'
                aria-label='Close trip details'
                onClick={closeDetails}
                disabled={isAddingTrip}
              >
                ×
              </button>

              <div className='trip-details__hero'>
                <img
                  src={selectedTrip.image}
                  alt={selectedTrip.destination}
                />

                <div className='trip-details__hero-overlay' />

                <div className='trip-details__hero-content'>
                  <span>{selectedTrip.destination}</span>
                  <h2 id='trip-details-title'>{selectedTrip.title}</h2>
                  <p>{selectedTrip.subtitle}</p>
                </div>
              </div>

              <div className='trip-details__body'>
                <div className='trip-details__overview'>
                  <span>{selectedTrip.duration} days</span>
                  <span>{selectedTrip.days.length} itinerary days</span>
                </div>

                <p className='trip-details__description'>
                  {selectedTrip.description}
                </p>

                <h3 className='trip-details__heading'>
                  Your day-by-day itinerary
                </h3>

                <div className='trip-details__days'>
                  {selectedTrip.days.map((day, index) => (
                    <article
                      className='trip-day'
                      key={day.title}
                    >
                      <div className='trip-day__number'>
                        {String(index + 1).padStart(2, "0")}
                      </div>

                      <div className='trip-day__content'>
                        <span className='trip-day__label'>DAY {index + 1}</span>

                        <h4>{day.title}</h4>
                        <p>{day.description}</p>

                        <ul>
                          {day.activities.map((activity) => (
                            <li key={activity}>{activity}</li>
                          ))}
                        </ul>
                      </div>
                    </article>
                  ))}
                </div>

                <div className='trip-details__footer'>
                  <div className='trip-details__add-form'>
                    <p>
                      Love this itinerary? Add this trip to your profile and
                      make it your own.
                    </p>

                    {!createdTripId && (
                      <>
                        <label htmlFor='inspiration-start-date'>
                          Start date
                        </label>

                        <DatePicker
                          id='inspiration-start-date'
                          name='startDate'
                          autoComplete='off'
                          selected={getDateFromString(startDate)}
                          onChange={(date: Date | null) => {
                            setStartDate(date ? getLocalDateString(date) : "");
                            setAddTripError("");
                            setAddTripMessage("");
                          }}
                          minDate={today}
                          dateFormat='dd MMM yyyy'
                          placeholderText='Select start date'
                          showPopperArrow={false}
                          className='create-trip__date-input inspiration-date-picker__input'
                          calendarClassName='create-trip__calendar'
                          popperClassName='inspiration-date-picker__popper'
                          popperPlacement='bottom-start'
                          disabled={isAddingTrip}
                          required
                        />

                        <button
                          className='trip-details__create-button'
                          type='button'
                          disabled={isAddingTrip}
                          onClick={handleAddTrip}
                        >
                          {isAddingTrip ? "Adding trip..." : "Add to my trips"}

                          {!isAddingTrip && <span aria-hidden='true'>→</span>}
                        </button>
                      </>
                    )}

                    {addTripError && (
                      <p
                        className='trip-details__feedback trip-details__feedback--error'
                        role='alert'
                      >
                        {addTripError}
                      </p>
                    )}

                    {addTripMessage && (
                      <p
                        className='trip-details__feedback trip-details__feedback--success'
                        role='status'
                      >
                        {addTripMessage}
                      </p>
                    )}

                    {createdTripId && (
                      <button
                        className='trip-details__create-button'
                        type='button'
                        onClick={() => navigate("/profile")}
                      >
                        View my trips
                        <span aria-hidden='true'>→</span>
                      </button>
                    )}

                    <p className='trip-details__note'>
                      Your trip will be saved as private. The displayed sample
                      activities are inspiration and are not automatically saved
                      to your trip itinerary.
                    </p>
                  </div>
                </div>
              </div>
            </section>
          </div>
        )}
      </main>
    </>
  );
}
