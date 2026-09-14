import { useState, type ChangeEvent, type FormEvent } from "react";
import { Header } from "../../components/Header";
import { Link } from "react-router-dom";
import DatePicker from "react-datepicker";

import { CitySelect } from "../../components/CitySelect";

import "react-datepicker/dist/react-datepicker.css";
import "./CreateTripPage.scss";
import type { TripForm } from "../../types/TripForm";
import type { CityOption } from "../../types/CityOption";

const API_URL = import.meta.env.VITE_API_URL;

const formatDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

export const CreateTripPage = () => {
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

  const [error, setError] = useState("");
  const [isCreated, setIsCreated] = useState(false);
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
    setIsCreated(false);
  };

  const handleDestinationChange = (destination: CityOption | null) => {
    setFormData((prev) => ({
      ...prev,
      destination,
    }));

    setError("");
    setIsCreated(false);
  };

  const handleStartDateChange = (date: Date | null) => {
    setFormData((prev) => ({
      ...prev,
      startDate: date,
      endDate:
        prev.endDate && date && prev.endDate < date ? null : prev.endDate,
    }));

    setError("");
    setIsCreated(false);
  };

  const handleEndDateChange = (date: Date | null) => {
    setFormData((prev) => ({
      ...prev,
      endDate: date,
    }));

    setError("");
    setIsCreated(false);
  };

  const handlePublicChange = (event: ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({
      ...prev,
      isPublic: event.target.checked,
    }));

    setError("");
    setIsCreated(false);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    setError("");
    setIsCreated(false);

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

    if (formData.budget && Number(formData.budget) < 0) {
      setError("Budget cannot be negative.");
      return;
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

    const tripData = {
      title: formData.name.trim(),
      destination: formData.destination.city.name,
      startDate: formatDate(formData.startDate),
      endDate: formatDate(formData.endDate),
      ...(formData.description.trim() && {
        description: formData.description.trim(),
      }),
      ...(formData.budget && {
        budget: Number(formData.budget),
      }),
      ...(formData.currency.trim() && {
        currency: formData.currency.trim(),
      }),
      isPublic: formData.isPublic,
    };

    try {
      setIsLoading(true);

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

        throw new Error(errorText || "Failed to create trip");
      }

      setIsCreated(true);
    } catch (error) {
      console.error("Failed to create trip:", error);
      setError("Failed to create trip. Please try again.");
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

          {isCreated && (
            <div className='create-trip__success'>
              <div className='create-trip__success-icon'>✓</div>

              <div>
                <h2>Trip created!</h2>

                <p>
                  Your trip to{" "}
                  <strong>{formData.destination?.city.name}</strong> has been
                  created successfully.
                </p>
              </div>
            </div>
          )}
        </section>
      </main>
    </>
  );
};
