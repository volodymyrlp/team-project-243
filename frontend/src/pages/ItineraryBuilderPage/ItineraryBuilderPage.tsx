import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import type { TripResponse } from "../../types/TripResponse";

import "./ItineraryBuilderPage.scss";

const API_URL = import.meta.env.VITE_API_URL;

const formatDate = (date: string) => {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

export const ItineraryBuilderPage = () => {
  const { tripId } = useParams();

  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
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
        const response = await fetch(`${API_URL}/api/v1/trips/${tripId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error("Failed to fetch trip.");
        }

        const data: TripResponse = await response.json();

        setTrip(data);
      } catch (error) {
        console.error("Failed to fetch trip:", error);
        setError("Failed to load trip. Please try again.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchTrip();
  }, [tripId]);

  if (isLoading) {
    return (
      <main className='itinerary-builder-page'>
        <section className='itinerary-builder'>
          <p>Loading trip...</p>
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
            ← Back to profile
          </Link>

          <p className='itinerary-builder__error'>
            {error || "Trip not found."}
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className='itinerary-builder-page'>
      <section className='itinerary-builder'>
        <Link
          to='/profile'
          className='itinerary-builder__back'
        >
          ← Back to profile
        </Link>

        <div className='itinerary-builder__header'>
          <span className='itinerary-builder__eyebrow'>YOUR TRIP</span>

          <h1>{trip.title}</h1>

          <p>{trip.destination}</p>

          <p>
            {formatDate(trip.startDate)} — {formatDate(trip.endDate)}
          </p>
        </div>
      </section>
    </main>
  );
};
