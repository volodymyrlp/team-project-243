import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import L from "leaflet";

import type { TripResponse } from "../../types/TripResponse";
import type { MapState } from "../../types/MapState";
import type { GeocodingResponse } from "../../types/GeocodingResponse";

import "leaflet/dist/leaflet.css";
import "./ItineraryBuilderPage.scss";

const API_URL = import.meta.env.VITE_API_URL;

const initialMapState: MapState = {
  coordinates: null,
  isLoading: false,
  error: "",
  destination: "",
};

const cityMarker = L.divIcon({
  className: "trip-map-marker-wrapper",
  html: '<div class="trip-map-marker"><span></span></div>',
  iconSize: [28, 36],
  iconAnchor: [14, 34],
  popupAnchor: [0, -32],
});

const formatDate = (date: string) =>
  new Date(`${date}T00:00:00`).toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });

const getTripDuration = (startDate: string, endDate: string) => {
  const start = new Date(`${startDate}T00:00:00Z`).getTime();
  const end = new Date(`${endDate}T00:00:00Z`).getTime();

  const days = Math.round((end - start) / 86_400_000) + 1;

  return Number.isFinite(days) && days > 0 ? days : null;
};

const formatBudget = (budget: number, currency: string | null) => {
  if (!currency?.trim()) {
    return new Intl.NumberFormat("en-GB", {
      maximumFractionDigits: 2,
    }).format(budget);
  }

  try {
    return new Intl.NumberFormat("en-GB", {
      style: "currency",
      currency: currency.toUpperCase(),
      maximumFractionDigits: 2,
    }).format(budget);
  } catch {
    return `${budget} ${currency.toUpperCase()}`;
  }
};

export const ItineraryBuilderPage = () => {
  const { tripId } = useParams();

  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const [mapState, setMapState] = useState<MapState>(initialMapState);

  const destination = trip?.destination?.trim() ?? "";

  useEffect(() => {
    const controller = new AbortController();

    const fetchTrip = async () => {
      if (!tripId) {
        setError("Trip ID is missing.");
        setIsLoading(false);
        return;
      }

      const token = localStorage.getItem("token");

      if (!token) {
        setError("You need to be logged in.");
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setError("");

        const response = await fetch(`${API_URL}/api/v1/trips/${tripId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(
            response.status === 404
              ? "Trip not found."
              : "Failed to fetch trip.",
          );
        }

        const data: TripResponse = await response.json();

        if (!controller.signal.aborted) {
          setTrip(data);
        }
      } catch (error) {
        if (error instanceof Error && error.name === "AbortError") {
          return;
        }

        console.error("Failed to fetch trip:", error);

        if (!controller.signal.aborted) {
          setError(
            error instanceof Error
              ? error.message
              : "Failed to load trip. Please try again.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    };

    void fetchTrip();

    return () => controller.abort();
  }, [tripId]);

  useEffect(() => {
    if (!destination) {
      return;
    }

    const controller = new AbortController();

    const fetchCoordinates = async () => {
      try {
        const params = new URLSearchParams({
          name: destination,
          count: "1",
          language: "en",
          format: "json",
        });

        const response = await fetch(
          `https://geocoding-api.open-meteo.com/v1/search?${params}`,
          { signal: controller.signal },
        );

        if (!response.ok) {
          throw new Error("Unable to find this city.");
        }

        const data: GeocodingResponse = await response.json();
        const city = data.results?.[0];

        if (!city) {
          throw new Error("We couldn't find this city on the map.");
        }

        if (!controller.signal.aborted) {
          setMapState({
            coordinates: {
              latitude: city.latitude,
              longitude: city.longitude,
              name: city.name,
              country: city.country,
            },
            isLoading: false,
            error: "",
            destination,
          });
        }
      } catch (error) {
        if (error instanceof Error && error.name === "AbortError") {
          return;
        }

        console.error("Failed to load city coordinates:", error);

        if (!controller.signal.aborted) {
          setMapState({
            coordinates: null,
            isLoading: false,
            error:
              error instanceof Error
                ? error.message
                : "The map is temporarily unavailable.",
            destination,
          });
        }
      }
    };

    void fetchCoordinates();

    return () => controller.abort();
  }, [destination]);

  const mapMatchesDestination =
    Boolean(destination) && mapState.destination === destination;

  const coordinates = mapMatchesDestination ? mapState.coordinates : null;

  const isMapLoading =
    Boolean(destination) && (!mapMatchesDestination || mapState.isLoading);

  const mapError = !destination
    ? "A destination is not available for this trip."
    : mapMatchesDestination
      ? mapState.error
      : "";

  if (isLoading) {
    return (
      <main className='itinerary-builder-page'>
        <section className='itinerary-builder'>
          <div className='itinerary-builder__message'>
            <span className='itinerary-builder__spinner' />
            <p>Loading your trip...</p>
          </div>
        </section>
      </main>
    );
  }

  if (error || !trip) {
    return (
      <main className='itinerary-builder-page'>
        <section className='itinerary-builder'>
          <Link
            to='/profile'
            className='itinerary-builder__back'
          >
            <span aria-hidden='true'>←</span> Back to profile
          </Link>

          <div className='itinerary-builder__error'>
            <h1>We couldn't open this trip</h1>
            <p>{error || "Trip not found."}</p>
          </div>
        </section>
      </main>
    );
  }

  const duration = getTripDuration(trip.startDate, trip.endDate);
  const description = trip.description?.trim();

  return (
    <main className='itinerary-builder-page'>
      <section className='itinerary-builder'>
        <Link
          to='/profile'
          className='itinerary-builder__back'
        >
          <span aria-hidden='true'>←</span> Back to profile
        </Link>

        <header className='itinerary-builder__hero'>
          <div className='itinerary-builder__cover'>
            {trip.coverUrl ? (
              <img
                src={trip.coverUrl}
                alt={`${trip.title} cover`}
              />
            ) : (
              <div className='itinerary-builder__cover-placeholder'>
                <span aria-hidden='true'>✈</span>
                <span>Your next adventure starts here</span>
              </div>
            )}

            <div className='itinerary-builder__cover-overlay' />
          </div>

          <div className='itinerary-builder__hero-content'>
            <span className='itinerary-builder__eyebrow'>YOUR TRIP</span>
            <h1>{trip.title}</h1>

            <p className='itinerary-builder__destination'>
              <span aria-hidden='true'>⌖</span>
              {trip.destination}
            </p>

            <div className='itinerary-builder__dates'>
              <span aria-hidden='true'>▦</span>
              <span>
                {formatDate(trip.startDate)} — {formatDate(trip.endDate)}
              </span>
            </div>
          </div>
        </header>

        <div className='itinerary-builder__content'>
          <section className='itinerary-builder__details'>
            <div className='itinerary-builder__section-heading'>
              <div>
                <span className='itinerary-builder__eyebrow'>
                  TRIP OVERVIEW
                </span>
                <h2>Your adventure at a glance</h2>
              </div>
            </div>

            <div className='itinerary-builder__stats'>
              {duration !== null && (
                <article className='itinerary-builder__stat'>
                  <span
                    className='itinerary-builder__stat-icon'
                    aria-hidden='true'
                  >
                    ◷
                  </span>
                  <div>
                    <span className='itinerary-builder__stat-label'>
                      Duration
                    </span>
                    <strong>
                      {duration} {duration === 1 ? "day" : "days"}
                    </strong>
                  </div>
                </article>
              )}

              {trip.budget !== null && trip.budget !== undefined && (
                <article className='itinerary-builder__stat'>
                  <span
                    className='itinerary-builder__stat-icon'
                    aria-hidden='true'
                  >
                    €
                  </span>
                  <div>
                    <span className='itinerary-builder__stat-label'>
                      Your budget
                    </span>
                    <strong>{formatBudget(trip.budget, trip.currency)}</strong>
                  </div>
                </article>
              )}
            </div>

            {description && (
              <article className='itinerary-builder__description'>
                <h2>About this trip</h2>
                <p>{description}</p>
              </article>
            )}
          </section>

          <section className='itinerary-builder__map-section'>
            <div className='itinerary-builder__section-heading'>
              <div>
                <span className='itinerary-builder__eyebrow'>
                  EXPLORE YOUR DESTINATION
                </span>
                <h2>Your destination</h2>
                <p>Get a feel for the city before your adventure begins.</p>
              </div>

              <span className='itinerary-builder__map-badge'>
                <span aria-hidden='true'>⌖</span> Map
              </span>
            </div>

            <div className='itinerary-builder__map'>
              {isMapLoading && (
                <div className='itinerary-builder__map-message'>
                  <span className='itinerary-builder__spinner' />
                  <p>Finding your destination...</p>
                </div>
              )}

              {!isMapLoading && coordinates && (
                <MapContainer
                  key={`${coordinates.latitude}-${coordinates.longitude}`}
                  center={[coordinates.latitude, coordinates.longitude]}
                  zoom={12}
                  scrollWheelZoom={false}
                  className='itinerary-builder__leaflet-map'
                >
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
                  />

                  <Marker
                    position={[coordinates.latitude, coordinates.longitude]}
                    icon={cityMarker}
                  >
                    <Popup>
                      <strong>{coordinates.name}</strong>
                      {coordinates.country && <div>{coordinates.country}</div>}
                    </Popup>
                  </Marker>
                </MapContainer>
              )}

              {!isMapLoading && !coordinates && (
                <div className='itinerary-builder__map-message'>
                  <span
                    className='itinerary-builder__map-message-icon'
                    aria-hidden='true'
                  >
                    ⌖
                  </span>
                  <p>{mapError || "The map is currently unavailable."}</p>
                  <span>You can still view the rest of your trip details.</span>
                </div>
              )}
            </div>

            {coordinates && (
              <p className='itinerary-builder__map-caption'>
                <span aria-hidden='true'>●</span>
                {coordinates.name}
                {coordinates.country ? `, ${coordinates.country}` : ""}
              </p>
            )}
          </section>
        </div>
      </section>
    </main>
  );
};
