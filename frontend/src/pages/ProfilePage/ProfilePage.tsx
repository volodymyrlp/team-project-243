import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./ProfilePage.scss";
import { Header } from "../../components/Header";
import type { UserProfile } from "../../types/UserProfile";
import type { UserUpdateRequest } from "../../types/UserUpdateRequest";
import type { TripResponse } from "../../types/TripResponse";

const MAX_AVATAR_SIZE = 5 * 1024 * 1024;

const ALLOWED_AVATAR_TYPES = ["image/jpeg", "image/png", "image/webp"];

type TripFilter = "all" | "favorites";

const getFavoriteStorageKey = (userId: string) =>
  `mriyaTrip_favoriteTrips_${userId}`;

const loadFavoriteTripIds = (userId: string): string[] => {
  try {
    const savedFavorites = localStorage.getItem(getFavoriteStorageKey(userId));

    if (!savedFavorites) {
      return [];
    }

    const parsed: unknown = JSON.parse(savedFavorites);

    return Array.isArray(parsed) && parsed.every((id) => typeof id === "string")
      ? parsed
      : [];
  } catch (error) {
    console.error("Failed to load favorite trips", error);
    return [];
  }
};

const saveFavoriteTripIds = (userId: string, ids: string[]) => {
  try {
    localStorage.setItem(getFavoriteStorageKey(userId), JSON.stringify(ids));
  } catch (error) {
    console.error("Failed to save favorite trips", error);
  }
};

export const ProfilePage = () => {
  const navigate = useNavigate();
  const avatarInputRef = useRef<HTMLInputElement>(null);

  const [profile, setProfile] = useState<UserProfile | null>(null);

  const [trips, setTrips] = useState<TripResponse[]>([]);
  const [isTripsLoading, setIsTripsLoading] = useState(true);
  const [tripsError, setTripsError] = useState("");
  const [deletingTripId, setDeletingTripId] = useState<string | null>(null);

  const [favoritesByUser, setFavoritesByUser] = useState<
    Record<string, string[]>
  >({});
  const [tripFilter, setTripFilter] = useState<TripFilter>("all");

  const favoriteTripIds = profile?.userId
    ? (favoritesByUser[profile.userId] ?? [])
    : [];

  const [editData, setEditData] = useState<UserUpdateRequest>({
    fullName: "",
    avatarUrl: null,
  });

  const [selectedAvatarFile, setSelectedAvatarFile] = useState<File | null>(
    null,
  );
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(null);

  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [isServerWaking, setIsServerWaking] = useState(false);

  useEffect(() => {
    if (!avatarPreviewUrl?.startsWith("blob:")) {
      return;
    }

    return () => {
      URL.revokeObjectURL(avatarPreviewUrl);
    };
  }, [avatarPreviewUrl]);

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

      const fetchProfileAttempt = async (): Promise<UserProfile | null> => {
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

        return (await response.json()) as UserProfile;
      };

      const applyProfile = (data: UserProfile) => {
        setProfile(data);

        setEditData({
          fullName: data.fullName,
          avatarUrl: null,
        });

        const savedFavorites = loadFavoriteTripIds(data.userId);

        setFavoritesByUser((previous) => ({
          ...previous,
          [data.userId]: savedFavorites,
        }));
      };

      let wakingTimer: ReturnType<typeof setTimeout> | undefined;

      try {
        wakingTimer = setTimeout(() => {
          if (isMounted) {
            setIsServerWaking(true);
          }
        }, 5000);

        try {
          const data = await fetchProfileAttempt();

          if (wakingTimer) {
            clearTimeout(wakingTimer);
          }

          if (!isMounted || !data) {
            return;
          }

          applyProfile(data);
        } catch (firstError) {
          if (wakingTimer) {
            clearTimeout(wakingTimer);
          }

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

            applyProfile(data);
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
        if (wakingTimer) {
          clearTimeout(wakingTimer);
        }

        if (isMounted) {
          setIsServerWaking(false);
          setIsLoading(false);
        }
      }
    };

    void fetchProfile();

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
      } catch (fetchError) {
        console.error("Trips load failed", fetchError);

        if (isMounted) {
          setTripsError("We couldn't load your trips. Please try again later.");
        }
      } finally {
        if (isMounted) {
          setIsTripsLoading(false);
        }
      }
    };

    void fetchTrips();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const handleToggleFavorite = (tripId: string) => {
    if (!profile?.userId) {
      return;
    }

    const userId = profile.userId;

    const nextIds = favoriteTripIds.includes(tripId)
      ? favoriteTripIds.filter((id) => id !== tripId)
      : [...favoriteTripIds, tripId];

    saveFavoriteTripIds(userId, nextIds);

    setFavoritesByUser((previous) => ({
      ...previous,
      [userId]: nextIds,
    }));
  };

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

      setTrips((previousTrips) =>
        previousTrips.filter((trip) => trip.tripId !== tripId),
      );

      if (profile?.userId) {
        const userId = profile.userId;
        const nextIds = favoriteTripIds.filter((id) => id !== tripId);

        saveFavoriteTripIds(userId, nextIds);

        setFavoritesByUser((previous) => ({
          ...previous,
          [userId]: nextIds,
        }));
      }
    } catch (deleteError) {
      console.error("Trip deletion failed", deleteError);

      setTripsError("We couldn't delete this trip. Please try again later.");
    } finally {
      setDeletingTripId(null);
    }
  };

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    setEditData((previousData) => ({
      ...previousData,
      [event.target.name]: event.target.value,
    }));
  };

  const handleAvatarChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    setError("");

    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      setError("Please select a JPEG, PNG, or WebP image.");
      event.target.value = "";
      return;
    }

    if (file.size > MAX_AVATAR_SIZE) {
      setError("The image must be 5 MB or smaller.");
      event.target.value = "";
      return;
    }

    setSelectedAvatarFile(file);
    setAvatarPreviewUrl(URL.createObjectURL(file));
  };

  const handleEdit = () => {
    if (!profile) {
      return;
    }

    setEditData({
      fullName: profile.fullName,
      avatarUrl: null,
    });

    setSelectedAvatarFile(null);
    setAvatarPreviewUrl(null);

    if (avatarInputRef.current) {
      avatarInputRef.current.value = "";
    }

    setError("");
    setIsEditing(true);
  };

  const handleCancel = () => {
    if (!profile) {
      return;
    }

    setEditData({
      fullName: profile.fullName,
      avatarUrl: null,
    });

    setSelectedAvatarFile(null);
    setAvatarPreviewUrl(null);

    if (avatarInputRef.current) {
      avatarInputRef.current.value = "";
    }

    setError("");
    setIsEditing(false);
  };

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const token = localStorage.getItem("token");

    if (!token || !profile) {
      return;
    }

    setIsSaving(true);
    setError("");

    try {
      const API_URL = import.meta.env.VITE_API_URL;
      let avatarKey: string | null = null;

      if (selectedAvatarFile) {
        const presignResponse = await fetch(
          `${API_URL}/api/v1/uploads/presign`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({
              contentType: selectedAvatarFile.type,
              fileSize: selectedAvatarFile.size,
            }),
          },
        );

        if (presignResponse.status === 401 || presignResponse.status === 403) {
          localStorage.removeItem("token");
          navigate("/login");
          return;
        }

        if (!presignResponse.ok) {
          throw new Error(
            `Failed to prepare avatar upload. Status: ${presignResponse.status}`,
          );
        }

        const presignData: {
          uploadUrl: string;
          key: string;
        } = await presignResponse.json();

        if (!presignData.uploadUrl || !presignData.key) {
          throw new Error("The server returned invalid upload information.");
        }

        const uploadResponse = await fetch(presignData.uploadUrl, {
          method: "PUT",
          headers: {
            "Content-Type": selectedAvatarFile.type,
          },
          body: selectedAvatarFile,
        });

        if (!uploadResponse.ok) {
          throw new Error(
            `Failed to upload avatar. Status: ${uploadResponse.status}`,
          );
        }

        avatarKey = presignData.key;
      }

      const response = await fetch(`${API_URL}/api/v1/users/me`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          fullName: editData.fullName,
          avatarUrl: avatarKey,
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
        avatarUrl: null,
      });

      setSelectedAvatarFile(null);
      setAvatarPreviewUrl(null);

      if (avatarInputRef.current) {
        avatarInputRef.current.value = "";
      }

      setIsEditing(false);
    } catch (saveError) {
      console.error("Profile update failed", saveError);
      setError(
        "We couldn't save your profile or upload the photo. Please try again.",
      );
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
  const displayedAvatarUrl = avatarPreviewUrl ?? profile.avatarUrl;

  const sortedTrips = [...trips].sort((tripA, tripB) => {
    const aIsFavorite = favoriteTripIds.includes(tripA.tripId);
    const bIsFavorite = favoriteTripIds.includes(tripB.tripId);

    if (aIsFavorite === bIsFavorite) {
      return 0;
    }

    return aIsFavorite ? -1 : 1;
  });

  const displayedTrips =
    tripFilter === "favorites"
      ? sortedTrips.filter((trip) => favoriteTripIds.includes(trip.tripId))
      : sortedTrips;

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
                {displayedAvatarUrl ? (
                  <img
                    src={displayedAvatarUrl}
                    alt={profile.fullName}
                  />
                ) : (
                  avatarLetter
                )}
              </div>

              {isEditing && (
                <>
                  <input
                    ref={avatarInputRef}
                    type='file'
                    accept='image/jpeg,image/png,image/webp'
                    onChange={handleAvatarChange}
                    hidden
                  />

                  <button
                    type='button'
                    className='profile-card__avatar-button'
                    onClick={() => avatarInputRef.current?.click()}
                    disabled={isSaving}
                  >
                    {selectedAvatarFile ? "Change photo" : "Choose photo"}
                  </button>
                </>
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
                  disabled={!isEditing || isSaving}
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

          {!isTripsLoading && !tripsError && trips.length > 0 && (
            <div className='profile-trips__filters'>
              <button
                type='button'
                className={`profile-trips__filter ${
                  tripFilter === "all" ? "profile-trips__filter--active" : ""
                }`}
                onClick={() => setTripFilter("all")}
                aria-pressed={tripFilter === "all"}
              >
                All trips ({trips.length})
              </button>

              <button
                type='button'
                className={`profile-trips__filter ${
                  tripFilter === "favorites"
                    ? "profile-trips__filter--active"
                    : ""
                }`}
                onClick={() => setTripFilter("favorites")}
                aria-pressed={tripFilter === "favorites"}
              >
                <span aria-hidden='true'>♥</span> Favorites (
                {
                  favoriteTripIds.filter((id) =>
                    trips.some((trip) => trip.tripId === id),
                  ).length
                }
                )
              </button>
            </div>
          )}

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
          ) : displayedTrips.length === 0 ? (
            <div className='profile-trips__empty'>
              <div className='profile-trips__empty-icon'>♥</div>

              <h3>No favorite trips yet</h3>

              <p>
                Select the heart on a trip card to add that trip to your
                favorites.
              </p>

              <button
                type='button'
                className='profile-trips__create-button'
                onClick={() => setTripFilter("all")}
              >
                Show all trips
              </button>
            </div>
          ) : (
            <div className='profile-trips__grid'>
              {displayedTrips.map((trip) => {
                const isFavorite = favoriteTripIds.includes(trip.tripId);

                return (
                  <div
                    className='trip-card'
                    key={trip.tripId}
                  >
                    <div className='trip-card__cover'>
                      {trip.coverUrl ? (
                        <img
                          src={`${import.meta.env.VITE_API_URL}${trip.coverUrl}`}
                          alt={`${trip.title} cover`}
                        />
                      ) : (
                        <div className='trip-card__cover-placeholder'>✈</div>
                      )}

                      <button
                        type='button'
                        className={`trip-card__favorite ${
                          isFavorite ? "trip-card__favorite--active" : ""
                        }`}
                        onClick={() => handleToggleFavorite(trip.tripId)}
                        aria-label={
                          isFavorite
                            ? `Remove ${trip.title} from favorites`
                            : `Add ${trip.title} to favorites`
                        }
                        aria-pressed={isFavorite}
                        title={
                          isFavorite
                            ? "Remove from favorites"
                            : "Add to favorites"
                        }
                      >
                        {isFavorite ? "♥" : "♡"}
                      </button>
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
                );
              })}
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
