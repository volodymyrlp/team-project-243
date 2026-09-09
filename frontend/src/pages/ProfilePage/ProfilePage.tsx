import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "./ProfilePage.scss";
import { Header } from "../../components/Header";
import type { UserProfile } from "../../types/UserProfile";
import type { UserUpdateRequest } from "../../types/UserUpdateRequest";

export const ProfilePage = () => {
  const [profile, setProfile] = useState<UserProfile | null>(null);

  const [editData, setEditData] = useState<UserUpdateRequest>({
    fullName: "",
    avatarUrl: null,
  });

  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      const token = localStorage.getItem("token");

      if (!token) {
        return;
      }

      try {
        const API_URL = import.meta.env.VITE_API_URL;

        const response = await fetch(`${API_URL}/api/v1/users/me`, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.status === 401 || response.status === 403) {
          localStorage.removeItem("token");
          window.location.href = "/login";

          return;
        }

        if (!response.ok) {
          throw new Error("Failed to fetch profile");
        }

        const data: UserProfile = await response.json();

        setProfile(data);

        setEditData({
          fullName: data.fullName,
          avatarUrl: data.avatarUrl,
        });
      } catch {
        setError("We couldn't load your profile. Please try again later.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, []);

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
        window.location.href = "/login";

        return;
      }

      if (!response.ok) {
        throw new Error("Failed to update profile");
      }

      const updatedProfile: UserProfile = await response.json();

      setProfile(updatedProfile);

      setEditData({
        fullName: updatedProfile.fullName,
        avatarUrl: updatedProfile.avatarUrl,
      });

      setIsEditing(false);
    } catch {
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

            <p>Loading your profile...</p>
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

          {profile.trips.length === 0 ? (
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
              {profile.trips.map((_, index) => (
                <div
                  className='trip-card'
                  key={index}
                >
                  <div className='trip-card__icon'>✈</div>

                  <div className='trip-card__content'>
                    <h3>Trip {index + 1}</h3>

                    <Link
                      to='/trips'
                      className='trip-card__button'
                    >
                      View trip
                    </Link>
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
