import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./ProfilePage.scss";
import { Header } from "../../components/Header";
import type { UserProfile } from "../../types/UserProfile";
import type { UserUpdateRequest } from "../../types/UserUpdateRequest";
import type { TripResponse } from "../../types/TripResponse";

export const ProfilePage = () => {
  const navigate = useNavigate();

  const [profile, setProfile] = useState<UserProfile | null>(null);

  const [trips, setTrips] = useState<TripResponse[]>([]);
  const [isTripsLoading, setIsTripsLoading] = useState(true);
  const [tripsError, setTripsError] = useState("");
  const [deletingTripId, setDeletingTripId] = useState<string | null>(null);

  const [editData, setEditData] = useState<UserUpdateRequest>({
    fullName: "",
    avatarUrl: null,
  });

  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [isServerWaking, setIsServerWaking] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const fetchProfile = async () => {
      const token = localStorage.getItem("token");

      if (!token) {
        if (isMounted) {
          setIsLoading(false);
        }

        return;
      }

      const API_URL = import.meta.env.VITE_API_URL;

      const fetchProfileAttempt = async () => {
        const response = await fetch(`${API_URL}/api/v1/users/me`, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.status === 401 || response.status === 403) {
          localStorage.removeItem("token");
          navigate("/login");

          return null;
        }

        if (!response.ok) {
          throw new Error(
            `Failed to fetch profile. Status: ${response.status}`,
          );
        }

        const data: UserProfile = await response.json();

        return data;
      };

      try {
        const wakingTimer = setTimeout(() => {
          if (isMounted) {
            setIsServerWaking(true);
          }
        }, 5000);

        try {
          const data = await fetchProfileAttempt();

          clearTimeout(wakingTimer);

          if (!isMounted || !data) {
            return;
          }

          setProfile(data);

          setEditData({
            fullName: data.fullName,
            avatarUrl: data.avatarUrl,
          });

          return;
        } catch (firstError) {
          clearTimeout(wakingTimer);

          console.error("Profile load failed", firstError);

          if (isMounted) {
            setIsServerWaking(true);
          }

          await new Promise((resolve) => setTimeout(resolve, 1000));

          try {
            const data = await fetchProfileAttempt();

            if (!isMounted || !data) {
              return;
            }

            setProfile(data);

            setEditData({
              fullName: data.fullName,
              avatarUrl: data.avatarUrl,
            });

            setError("");
          } catch (secondError) {
            console.error("Profile load retry failed", secondError);

            if (isMounted) {
              setError(
                "We couldn't load your profile. Please try again later.",
              );
            }
          }
        }
      } finally {
        if (isMounted) {
          setIsServerWaking(false);
          setIsLoading(false);
        }
      }
    };

    fetchProfile();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  useEffect(() => {
    let isMounted = true;

    const fetchTrips = async () => {
      const token = localStorage.getItem("token");

      if (!token) {
        if (isMounted) {
          setIsTripsLoading(false);
        }

        return;
      }

      try {
        const API_URL = import.meta.env.VITE_API_URL;

        const response = await fetch(`${API_URL}/api/v1/trips`, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.status === 401 || response.status === 403) {
          localStorage.removeItem("token");
          navigate("/login");

          return;
        }

        if (!response.ok) {
          throw new Error(`Failed to fetch trips. Status: ${response.status}`);
        }

        const data: { content: TripResponse[] } = await response.json();

        if (!isMounted) {
          return;
        }

        setTrips(data.content);
        setTripsError("");
      } catch (e) {
        console.error("Trips load failed", e);

        if (isMounted) {
          setTripsError("We couldn't load your trips. Please try again later.");
        }
      } finally {
        if (isMounted) {
          setIsTripsLoading(false);
        }
      }
    };

    fetchTrips();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const handleDeleteTrip = async (tripId: string, tripTitle: string) => {
    const shouldDelete = window.confirm(
      `Are you sure you want to delete "${tripTitle}"?`,
    );

    if (!shouldDelete) {
      return;
    }

    const token = localStorage.getItem("token");

    if (!token) {
      return;
    }

    setDeletingTripId(tripId);
    setTripsError("");

    try {
      const API_URL = import.meta.env.VITE_API_URL;

      const response = await fetch(`${API_URL}/api/v1/trips/${tripId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        navigate("/login");

        return;
      }

      if (!response.ok) {
        throw new Error(`Failed to delete trip. Status: ${response.status}`);
      }

      setTrips((prevTrips) =>
        prevTrips.filter((trip) => trip.tripId !== tripId),
      );
    } catch (e) {
      console.error("Trip deletion failed", e);

      setTripsError("We couldn't delete this trip. Please try again later.");
    } finally {
      setDeletingTripId(null);
    }
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setEditData((prev) => ({
      ...prev,
      [event.target.name]: event.target.value,
    }));
  };

  const handleEdit = () => {
    if (!profile) {
      return;
    }

    setEditData({
      fullName: profile.fullName,
      avatarUrl: profile.avatarUrl,
    });

    setError("");
    setIsEditing(true);
  };

  const handleCancel = () => {
    if (!profile) {
      return;
    }

    setEditData({
      fullName: profile.fullName,
      avatarUrl: profile.avatarUrl,
    });

    setError("");
    setIsEditing(false);
  };

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault();

    const token = localStorage.getItem("token");

    if (!token || !profile) {
      return;
    }

    setIsSaving(true);
    setError("");

    try {
      const API_URL = import.meta.env.VITE_API_URL;

      const response = await fetch(`${API_URL}/api/v1/users/me`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          fullName: editData.fullName,
          avatarUrl: editData.avatarUrl,
        }),
      });

      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        navigate("/login");

        return;
      }

      if (!response.ok) {
        throw new Error(`Failed to update profile. Status: ${response.status}`);
      }

      const updatedProfile: UserProfile = await response.json();

      setProfile(updatedProfile);

      setEditData({
        fullName: updatedProfile.fullName,
        avatarUrl: updatedProfile.avatarUrl,
      });

      setIsEditing(false);
    } catch (e) {
      console.error("Profile update failed", e);
      setError("We couldn't update your profile. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <main className='profile-page'>
        <Header />

        <section className='profile-page__content'>
          <div className='profile-page__loading'>
            <div className='profile-page__loading-spinner' />

            <p>
              {isServerWaking
                ? "Server is waking up. This may take a little longer..."
                : "Loading your profile..."}
            </p>
          </div>
        </section>
      </main>
    );
  }

  if (error && !profile) {
    return (
      <main className='profile-page'>
        <Header />

        <section className='profile-page__content'>
          <div className='profile-page__error'>
            <div className='profile-page__error-icon'>!</div>

            <h2>Something went wrong</h2>

            <p>{error}</p>
          </div>
        </section>
      </main>
    );
  }

  if (!profile) {
    return null;
  }

  const avatarLetter = profile.fullName.charAt(0).toUpperCase();

  return (
    <main className='profile-page'>
      <Header />

      <section className='profile-page__content'>
        <div className='profile-page__heading'>
          <span className='profile-page__eyebrow'>YOUR ACCOUNT</span>

          <h1>Profile</h1>

          <p>
            Manage your personal information and keep your travel profile up to
            date.
          </p>
        </div>

        <div className='profile-card'>
          <div className='profile-card__top'>
            <div className='profile-card__avatar-wrapper'>
              <div className='profile-card__avatar'>
                {profile.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.fullName}
                  />
                ) : (
                  avatarLetter
                )}
              </div>

              {isEditing && (
                <button
                  type='button'
                  className='profile-card__avatar-button'
                  disabled
                >
                  Change photo
                </button>
              )}
            </div>

            <div className='profile-card__user'>
              <h2>{profile.fullName}</h2>

              <p>{profile.email}</p>
            </div>

            {!isEditing && (
              <button
                type='button'
                className='profile-card__edit-button'
                onClick={handleEdit}
              >
                Edit profile
              </button>
            )}
          </div>

          <div className='profile-card__divider' />

          <form
            className='profile-form'
            onSubmit={handleSave}
          >
            <div className='profile-form__heading'>
              <h3>Personal information</h3>

              <p>
                This information will be used to personalize your travel
                experience.
              </p>
            </div>

            <div className='profile-form__grid'>
              <label>
                <span>Full name</span>

                <input
                  name='fullName'
                  type='text'
                  value={isEditing ? editData.fullName : profile.fullName}
                  onChange={handleChange}
                  disabled={!isEditing}
                  required
                />
              </label>

              <label>
                <span>Email</span>

                <input
                  type='email'
                  value={profile.email}
                  disabled
                />
              </label>
            </div>

            {error && <p className='profile-form__error'>{error}</p>}

            {isEditing && (
              <div className='profile-form__actions'>
                <button
                  type='button'
                  className='profile-form__cancel'
                  onClick={handleCancel}
                  disabled={isSaving}
                >
                  Cancel
                </button>

                <button
                  type='submit'
                  className='profile-form__save'
                  disabled={isSaving}
                >
                  {isSaving ? "Saving..." : "Save changes"}
                </button>
              </div>
            )}
          </form>
        </div>

        <section className='profile-trips'>
          <div className='profile-trips__heading'>
            <div>
              <span className='profile-page__eyebrow'>YOUR TRAVEL</span>

              <h2>My trips</h2>

              <p>All trips you have created in one place.</p>
            </div>

            <Link
              to='/trips/create'
              className='profile-trips__create-button'
            >
              Create a trip
            </Link>
          </div>

          {isTripsLoading ? (
            <div className='profile-trips__loading'>
              <p>Loading your trips...</p>
            </div>
          ) : tripsError ? (
            <div className='profile-trips__error'>
              <p>{tripsError}</p>
            </div>
          ) : trips.length === 0 ? (
            <div className='profile-trips__empty'>
              <div className='profile-trips__empty-icon'>✈</div>

              <h3>No trips yet</h3>

              <p>
                You haven't created any trips yet. Start planning your next
                adventure and it will appear here.
              </p>

              <Link
                to='/trips/create'
                className='profile-trips__create-button'
              >
                Create your first trip
              </Link>
            </div>
          ) : (
            <div className='profile-trips__grid'>
              {trips.map((trip) => (
                <div
                  className='trip-card'
                  key={trip.tripId}
                >
                  <div className='trip-card__cover'>
                    {trip.coverUrl ? (
                      <img
                        src={trip.coverUrl}
                        alt={`${trip.title} cover`}
                      />
                    ) : (
                      <div className='trip-card__cover-placeholder'>✈</div>
                    )}
                  </div>

                  <div className='trip-card__body'>
                    <div className='trip-card__content'>
                      <div className='trip-card__details'>
                        <h3>{trip.title}</h3>

                        <p className='trip-card__destination'>
                          {trip.destination}
                        </p>

                        <span
                          className={`trip-card__status ${
                            trip.isPublic
                              ? "trip-card__status--public"
                              : "trip-card__status--private"
                          }`}
                        >
                          {trip.isPublic ? "Public" : "Private"}
                        </span>
                      </div>

                      <div className='trip-card__actions'>
                        <Link
                          to={`/trips/${trip.tripId}/itinerary`}
                          className='trip-card__button'
                        >
                          View trip
                        </Link>

                        <button
                          type='button'
                          className='trip-card__delete'
                          onClick={() =>
                            handleDeleteTrip(trip.tripId, trip.title)
                          }
                          disabled={deletingTripId === trip.tripId}
                        >
                          {deletingTripId === trip.tripId
                            ? "Deleting..."
                            : "Delete"}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <div className='profile-page__travel-card'>
          <div>
            <span className='profile-page__travel-icon'>✈</span>

            <div>
              <h3>Ready for your next adventure?</h3>

              <p>
                Create a trip and start planning your next unforgettable
                journey.
              </p>
            </div>
          </div>

          <Link
            to='/trips/create'
            className='profile-page__travel-button'
          >
            Plan a trip
          </Link>
        </div>
      </section>
    </main>
  );
};
