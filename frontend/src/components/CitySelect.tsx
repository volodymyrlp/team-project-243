import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type KeyboardEvent,
} from "react";
import type { CitySelectProps } from "../types/CitySelectProps";
import type { CityOption } from "../types/CityOption";
import type { City } from "../types/City";


type GeocodingResult = {
  id: number;
  name: string;
  country: string;
  country_code: string;
  admin1?: string;
  latitude: number;
  longitude: number;
};

type GeocodingResponse = {
  results?: GeocodingResult[];
};

export const CitySelect = ({
  value,
  onChange,
}: CitySelectProps) => {
  const [inputValue, setInputValue] = useState(value?.label ?? "");
  const [options, setOptions] = useState<CityOption[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const search = inputValue.trim();

    if (search.length < 3) {
      return;
    }

    const timeoutId = setTimeout(async () => {
      try {
        setIsLoading(true);

        const response = await fetch(
          `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(
            search,
          )}&count=8&language=en&format=json`,
        );

        if (!response.ok) {
          throw new Error("Failed to fetch cities");
        }

        const data: GeocodingResponse = await response.json();

        const cityOptions: CityOption[] = (data.results ?? []).map(
          (city) => {
            const cityData: City = {
              id: city.id,
              name: city.name,
              country: city.country,
              countryCode: city.country_code,
              region: city.admin1,
              latitude: city.latitude,
              longitude: city.longitude,
            };

            return {
              value: city.id,
              label: cityData.region
                ? `${cityData.name}, ${cityData.region}, ${cityData.country}`
                : `${cityData.name}, ${cityData.country}`,
              city: cityData,
            };
          },
        );

        setOptions(cityOptions);
      } catch (error) {
        console.error("Failed to fetch cities:", error);
        setOptions([]);
      } finally {
        setIsLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [inputValue]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleInputChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const newValue = event.target.value;

    setInputValue(newValue);

    if (value) {
      onChange(null);
    }

    if (newValue.trim().length >= 3) {
      setOptions([]);
      setIsOpen(true);
    } else {
      setOptions([]);
      setIsOpen(false);
      setIsLoading(false);
    }
  };

  const handleCitySelect = (option: CityOption) => {
    setInputValue(option.label);
    setOptions([]);
    setIsOpen(false);

    onChange(option);
  };

  const handleClear = () => {
    setInputValue("");
    setOptions([]);
    setIsOpen(false);
    setIsLoading(false);

    onChange(null);
  };

  const handleKeyDown = (
    event: KeyboardEvent<HTMLInputElement>,
  ) => {
    if (event.key === "Escape") {
      setIsOpen(false);
    }
  };

  return (
    <div
      ref={containerRef}
      className='city-select'
    >
      <div className='city-select__input-wrapper'>
        <input
          id='destination'
          type='text'
          value={inputValue}
          onChange={handleInputChange}
          onFocus={() => {
            if (inputValue.trim().length >= 3) {
              setIsOpen(true);
            }
          }}
          onKeyDown={handleKeyDown}
          placeholder='Search city...'
          autoComplete='off'
          className='city-select__input'
        />

        {isLoading && (
          <span className='city-select__loader' />
        )}

        {inputValue && !isLoading && (
          <button
            type='button'
            className='city-select__clear'
            onClick={handleClear}
            aria-label='Clear destination'
          >
            ×
          </button>
        )}
      </div>

      {isOpen && (
        <div className='city-select__dropdown'>
          {isLoading ? (
            <div className='city-select__message'>
              Searching cities...
            </div>
          ) : options.length > 0 ? (
            options.map((option) => (
              <button
                key={option.value}
                type='button'
                className='city-select__option'
                onClick={() => handleCitySelect(option)}
              >
                <span className='city-select__city'>
                  {option.city.name}
                </span>

                <span className='city-select__location'>
                  {option.city.region
                    ? `${option.city.region}, ${option.city.country}`
                    : option.city.country}
                </span>
              </button>
            ))
          ) : (
            <div className='city-select__message'>
              No cities found
            </div>
          )}
        </div>
      )}
    </div>
  );
};