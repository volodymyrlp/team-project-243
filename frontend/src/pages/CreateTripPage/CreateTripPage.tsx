import { useState, type ChangeEvent, type FormEvent } from "react";
import { Header } from "../../components/Header";
import { Link, useNavigate } from "react-router-dom";
import DatePicker from "react-datepicker";

import { CitySelect } from "../../components/CitySelect";

import "react-datepicker/dist/react-datepicker.css";
import "./CreateTripPage.scss";
import type { TripForm } from "../../types/TripForm";
import type { CityOption } from "../../types/CityOption";
import type { TripResponse } from "../../types/TripResponse";

const API_URL = import.meta.env.VITE_API_URL;
const MAX_COVER_SIZE = 5 * 1024 * 1024;
interface PresignResponse {
  uploadUrl: string;
  key: string;
}

const formatDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

const getErrorMessage = (responseBody: string, fallback: string): string => {
  if (!responseBody.trim()) {
    return fallback;
  }

  try {
    const parsed: unknown = JSON.parse(responseBody);

    if (typeof parsed === "string") {
      return parsed;
    }

    if (typeof parsed === "object" && parsed !== null) {
      const data = parsed as {
        message?: unknown;
        error?: unknown;
        errors?: unknown;
      };

      if (Array.isArray(data.errors)) {
        const messages = data.errors.filter(
          (item): item is string => typeof item === "string",
        );

        if (messages.length > 0) {
          return messages.join(" ");
        }
      }

      if (typeof data.message === "string" && data.message.trim()) {
        return data.message;
      }

      if (typeof data.error === "string" && data.error.trim()) {
        return data.error;
      }
    }
  } catch {
    // The response is not JSON; use the text returned by the server.
  }

  return responseBody;
};

export const CreateTripPage = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState<TripForm>({
    name: "",
    destination: null,
    startDate: null,
    endDate: null,
    description: "",
    budget: "",
    currency: "UAH",
    isPublic: false,
  });

  const [shareEmail, setShareEmail] = useState("");
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    setError("");
  };

  const handleDestinationChange = (destination: CityOption | null) => {
    setFormData((prev) => ({
      ...prev,
      destination,
    }));

    setError("");
  };

  const handleStartDateChange = (date: Date | null) => {
    setFormData((prev) => ({
      ...prev,
      startDate: date,
      endDate:
        prev.endDate && date && prev.endDate < date ? null : prev.endDate,
    }));

    setError("");
  };

  const handleEndDateChange = (date: Date | null) => {
    setFormData((prev) => ({
      ...prev,
      endDate: date,
    }));

    setError("");
  };

  const handlePublicChange = (event: ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({
      ...prev,
      isPublic: event.target.checked,
    }));

    setError("");
  };

  const handleShareEmailChange = (event: ChangeEvent<HTMLInputElement>) => {
    setShareEmail(event.target.value);
    setError("");
  };

  const handleCoverChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      setError("Please select an image file.");
      event.target.value = "";
      return;
    }

    if (file.size > MAX_COVER_SIZE) {
      setError("Cover photo must not exceed 5 MB.");
      event.target.value = "";
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result !== "string") {
        setError("Failed to read the selected image. Please try again.");
        return;
      }

      setCoverFile(file);
      setCoverPreview(reader.result);
      setError("");
    };

    reader.onerror = () => {
      setError("Failed to read the selected image. Please try again.");
      event.target.value = "";
    };

    reader.readAsDataURL(file);
    event.target.value = "";
  };

  const handleRemoveCover = () => {
    setCoverFile(null);
    setCoverPreview("");
    setError("");
  };

  const uploadCover = async (token: string): Promise<string | undefined> => {
    if (!coverFile) {
      return undefined;
    }

    const presignResponse = await fetch(`${API_URL}/api/v1/uploads/presign`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        contentType: coverFile.type,
        fileSize: coverFile.size,
      }),
    });

    if (!presignResponse.ok) {
      const errorText = await presignResponse.text();

      throw new Error(
        getErrorMessage(errorText, "Failed to prepare cover upload."),
      );
    }

    const presignData: PresignResponse = await presignResponse.json();
    const { uploadUrl, key } = presignData;

    if (
      typeof uploadUrl !== "string" ||
      !uploadUrl.trim() ||
      typeof key !== "string" ||
      !key.trim()
    ) {
      throw new Error("The server returned invalid upload information.");
    }

    const uploadResponse = await fetch(uploadUrl, {
      method: "PUT",
      headers: {
        "Content-Type": coverFile.type,
      },
      body: coverFile,
    });

    if (!uploadResponse.ok) {
      throw new Error(
        `Failed to upload cover photo (HTTP ${uploadResponse.status}).`,
      );
    }

    return key;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (isLoading) {
      return;
    }

    if (!API_URL) {
      setError("API URL is not configured. Please try again later.");
      return;
    }

    if (!formData.destination) {
      setError("Please select a destination.");
      return;
    }

    if (!formData.startDate || !formData.endDate) {
      setError("Please select your travel dates.");
      return;
    }

    if (formData.endDate < formData.startDate) {
      setError("End date cannot be earlier than start date.");
      return;
    }

    if (!formData.name.trim()) {
      setError("Please enter a trip name.");
      return;
    }

    if (formData.name.trim().length > 255) {
      setError("Trip name cannot exceed 255 characters.");
      return;
    }

    if (formData.budget.trim() !== "") {
      const budget = Number(formData.budget);

      if (!Number.isFinite(budget)) {
        setError("Please enter a valid budget.");
        return;
      }

      if (budget < 0) {
        setError("Budget cannot be negative.");
        return;
      }
    }

    if (formData.currency.trim().length > 10) {
      setError("Currency cannot exceed 10 characters.");
      return;
    }

    const token = localStorage.getItem("token");

    if (!token) {
      setError("You need to be logged in to create a trip.");
      return;
    }

    try {
      setIsLoading(true);

      const coverKey = await uploadCover(token);

      const tripData = {
        title: formData.name.trim(),
        destination: formData.destination.city.name,
        startDate: formatDate(formData.startDate),
        endDate: formatDate(formData.endDate),
        ...(formData.description.trim() && {
          description: formData.description.trim(),
        }),
        ...(formData.budget.trim() !== "" && {
          budget: Number(formData.budget),
        }),
        ...(formData.currency.trim() && {
          currency: formData.currency.trim(),
        }),
        isPublic: formData.isPublic,
        ...(coverKey && {
          coverUrl: coverKey,
        }),
      };

      const response = await fetch(`${API_URL}/api/v1/trips`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(tripData),
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(getErrorMessage(errorText, "Failed to create trip."));
      }

      const createdTrip: TripResponse = await response.json();

      if (!createdTrip.tripId) {
        throw new Error("The server created the trip but returned no trip ID.");
      }

      navigate(`/trips/${createdTrip.tripId}/itinerary`);
    } catch (error) {
      console.error("Failed to create trip:", error);

      setError(
        error instanceof Error
          ? error.message
          : "Failed to create trip. Please try again.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Header />

      <main className='create-trip-page'>
        <section className='create-trip'>
          <div className='create-trip__header'>
            <Link
              to='/profile'
              className='create-trip__back'
            >
              ← Back to profile
            </Link>

            <span className='create-trip__eyebrow'>YOUR TRAVEL</span>

            <h1>Create a trip</h1>

            <p>
              Plan your next adventure by adding a destination and trip details.
            </p>
          </div>

          <form
            className='create-trip__form'
            onSubmit={handleSubmit}
          >
            <div className='create-trip__field'>
              <label htmlFor='name'>Trip name</label>

              <input
                id='name'
                name='name'
                type='text'
                placeholder='Summer vacation in Paris'
                value={formData.name}
                onChange={handleChange}
                maxLength={255}
                required
              />
            </div>

            <div className='create-trip__field'>
              <label htmlFor='destination'>Destination</label>

              <CitySelect
                value={formData.destination}
                onChange={handleDestinationChange}
              />
            </div>

            <div className='create-trip__dates'>
              <div className='create-trip__field'>
                <label htmlFor='start-date'>Start date</label>

                <DatePicker
                  id='start-date'
                  name='startDate'
                  autoComplete='off'
                  selected={formData.startDate}
                  onChange={handleStartDateChange}
                  minDate={new Date()}
                  dateFormat='dd MMM yyyy'
                  placeholderText='Select start date'
                  showPopperArrow={false}
                  isClearable
                  className='create-trip__date-input'
                  calendarClassName='create-trip__calendar'
                />
              </div>

              <div className='create-trip__field'>
                <label htmlFor='end-date'>End date</label>

                <DatePicker
                  id='end-date'
                  name='endDate'
                  autoComplete='off'
                  selected={formData.endDate}
                  onChange={handleEndDateChange}
                  minDate={formData.startDate || new Date()}
                  dateFormat='dd MMM yyyy'
                  placeholderText='Select end date'
                  showPopperArrow={false}
                  isClearable
                  className='create-trip__date-input'
                  calendarClassName='create-trip__calendar'
                />
              </div>
            </div>

            <div className='create-trip__field'>
              <label htmlFor='description'>Description</label>

              <textarea
                id='description'
                name='description'
                placeholder='Tell something about your trip...'
                value={formData.description}
                onChange={handleChange}
              />
            </div>

            <div className='create-trip__field'>
              <label htmlFor='share-email'>
                Email to share with <span>(optional)</span>
              </label>

              <input
                id='share-email'
                name='shareEmail'
                type='email'
                placeholder='friend@example.com'
                value={shareEmail}
                onChange={handleShareEmailChange}
                autoComplete='email'
              />

              <small>
                Enter an email address for the person you want to share this
                trip with.
              </small>
            </div>

            <div className='create-trip__field'>
              <label htmlFor='cover'>Cover photo</label>

              {coverPreview ? (
                <div className='create-trip__cover-preview'>
                  <img
                    src={coverPreview}
                    alt='Selected trip cover preview'
                  />

                  <button
                    type='button'
                    className='create-trip__cover-remove'
                    onClick={handleRemoveCover}
                  >
                    Remove photo
                  </button>
                </div>
              ) : (
                <label
                  htmlFor='cover'
                  className='create-trip__cover-upload'
                >
                  <span className='create-trip__cover-icon'>📷</span>

                  <span className='create-trip__cover-title'>
                    Choose a cover photo
                  </span>

                  <span className='create-trip__cover-hint'>
                    JPG, PNG or other image · Max 5 MB
                  </span>

                  <input
                    id='cover'
                    type='file'
                    accept='image/*'
                    onChange={handleCoverChange}
                  />
                </label>
              )}
            </div>

            <div className='create-trip__dates'>
              <div className='create-trip__field'>
                <label htmlFor='budget'>Budget</label>

                <input
                  id='budget'
                  name='budget'
                  type='number'
                  min='0'
                  step='0.01'
                  placeholder='15000'
                  value={formData.budget}
                  onChange={handleChange}
                />
              </div>

              <div className='create-trip__field'>
                <label htmlFor='currency'>Currency</label>

                <input
                  id='currency'
                  name='currency'
                  type='text'
                  maxLength={10}
                  placeholder='UAH'
                  value={formData.currency}
                  onChange={handleChange}
                />
              </div>
            </div>

            <label className='create-trip__checkbox'>
              <input
                type='checkbox'
                checked={formData.isPublic}
                onChange={handlePublicChange}
              />

              <span>Make this trip public</span>
            </label>

            {error && <p className='create-trip__error'>{error}</p>}

            <button
              type='submit'
              className='create-trip__submit'
              disabled={isLoading}
            >
              {isLoading ? "Creating..." : "Create trip"}
            </button>
          </form>
        </section>
      </main>
    </>
  );
};
