import { useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import type { LatLngBoundsExpression } from "leaflet";
import type { TripResponse } from "../../types/TripResponse";

import "leaflet/dist/leaflet.css";
import "./ProfileTravelMap.scss";

interface ProfileTravelMapProps {
  trips: TripResponse[];
  isLoading: boolean;
}

interface Coordinates {
  latitude: number;
  longitude: number;
}

interface GeocodingResult {
  latitude: number;
  longitude: number;
  name: string;
  country?: string;
  admin1?: string;
}

interface GeocodingResponse {
  results?: GeocodingResult[];
}

interface Destination {
  key: string;
  name: string;
}

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 2;

// Та сама іконка маркера, що й у WorldMap.
const markerIcon = L.icon({
  iconUrl: new URL("leaflet/dist/images/marker-icon.png", import.meta.url).href,
  iconRetinaUrl: new URL(
    "leaflet/dist/images/marker-icon-2x.png",
    import.meta.url,
  ).href,
  shadowUrl: new URL("leaflet/dist/images/marker-shadow.png", import.meta.url)
    .href,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

const normalizeDestination = (destination: string) =>
  destination.trim().toLocaleLowerCase();

const getDestinationQuery = (destination: string) => {
  const parts = destination.split(",").map((part) => part.trim());

  return {
    city: parts[0] ?? destination,
    country: parts.length > 1 ? parts[parts.length - 1] : "",
  };
};

const buildGeocodingQuery = (destination: string) => {
  const { city, country } = getDestinationQuery(destination);
  return country ? `${city}, ${country}` : city;
};

function FitMapToMarkers({ coordinates }: { coordinates: Coordinates[] }) {
  const map = useMap();

  useEffect(() => {
    if (coordinates.length === 0) return;

    if (coordinates.length === 1) {
      map.setView([coordinates[0].latitude, coordinates[0].longitude], 6);
      return;
    }

    const bounds: LatLngBoundsExpression = coordinates.map(
      ({ latitude, longitude }) => [latitude, longitude],
    );

    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 6 });
  }, [coordinates, map]);

  return null;
}

export default function ProfileTravelMap({
  trips,
  isLoading,
}: ProfileTravelMapProps) {
  const [coordinatesByDestination, setCoordinatesByDestination] = useState<
    Record<string, Coordinates>
  >({});
  const [failedDestinations, setFailedDestinations] = useState<string[]>([]);
  const [pendingDestinations, setPendingDestinations] = useState<string[]>([]);

  const uniqueDestinations = useMemo<Destination[]>(() => {
    const destinations = new Map<string, string>();

    trips.forEach((trip) => {
      const destination = trip.destination.trim();
      const key = normalizeDestination(destination);

      if (destination && !destinations.has(key)) {
        destinations.set(key, destination);
      }
    });

    return Array.from(destinations.entries()).map(([key, name]) => ({
      key,
      name,
    }));
  }, [trips]);

  useEffect(() => {
    const controller = new AbortController();

    const missingDestinations = uniqueDestinations.filter(
      ({ key }) =>
        !(key in coordinatesByDestination) && !failedDestinations.includes(key),
    );

    if (missingDestinations.length === 0) {
      return () => controller.abort();
    }

    let active = true;

    const resolveCoordinates = async () => {
      setPendingDestinations((previous) => [
        ...new Set([...previous, ...missingDestinations.map(({ key }) => key)]),
      ]);

      const resolved: Record<string, Coordinates> = {};
      const failed: string[] = [];

      try {
        for (const destination of missingDestinations) {
          if (controller.signal.aborted) return;

          try {
            const query = buildGeocodingQuery(destination.name);

            const params = new URLSearchParams({
              name: query,
              count: "10",
              language: "en",
              format: "json",
            });

            const response = await fetch(
              `https://geocoding-api.open-meteo.com/v1/search?${params}`,
              { signal: controller.signal },
            );

            if (!response.ok) {
              throw new Error(`Geocoding failed: ${response.status}`);
            }

            const data: GeocodingResponse = await response.json();
            const results = data.results ?? [];
            const { country } = getDestinationQuery(destination.name);

            const matchingResult = country
              ? results.find(
                  (result) =>
                    result.country?.toLocaleLowerCase() ===
                    country.toLocaleLowerCase(),
                )
              : undefined;

            const result = matchingResult ?? results[0];

            if (!result) {
              failed.push(destination.key);
              continue;
            }

            resolved[destination.key] = {
              latitude: result.latitude,
              longitude: result.longitude,
            };
          } catch (error) {
            if (controller.signal.aborted) return;

            console.error(
              `Failed to locate destination "${destination.name}"`,
              error,
            );

            failed.push(destination.key);
          }
        }

        if (!active || controller.signal.aborted) return;

        setCoordinatesByDestination((previous) => ({
          ...previous,
          ...resolved,
        }));

        setFailedDestinations((previous) => [
          ...new Set([...previous, ...failed]),
        ]);
      } finally {
        if (active && !controller.signal.aborted) {
          setPendingDestinations((previous) =>
            previous.filter(
              (key) =>
                !missingDestinations.some(
                  (destination) => destination.key === key,
                ),
            ),
          );
        }
      }
    };

    void Promise.resolve().then(resolveCoordinates);

    return () => {
      active = false;
      controller.abort();
    };
  }, [uniqueDestinations, coordinatesByDestination, failedDestinations]);

  const markers = useMemo(
    () =>
      trips.flatMap((trip) => {
        const key = normalizeDestination(trip.destination);
        const coordinates = coordinatesByDestination[key];

        if (!coordinates) return [];

        return [{ trip, coordinates, key }];
      }),
    [trips, coordinatesByDestination],
  );

  const markerCoordinates = useMemo(
    () => markers.map(({ coordinates }) => coordinates),
    [markers],
  );

  const isGeocoding = pendingDestinations.some((key) =>
    uniqueDestinations.some((destination) => destination.key === key),
  );

  if (isLoading) {
    return (
      <section className='profile-travel-map'>
        <div className='profile-travel-map__header'>
          <h2 className='profile-travel-map__title'>Your travel map</h2>
          <p className='profile-travel-map__description'>
            Your destinations will appear here.
          </p>
        </div>

        <div className='profile-travel-map__loading'>Loading your trips...</div>
      </section>
    );
  }

  if (trips.length === 0) {
    return (
      <section className='profile-travel-map'>
        <div className='profile-travel-map__header'>
          <h2 className='profile-travel-map__title'>Your travel map</h2>
          <p className='profile-travel-map__description'>
            Create your first trip to see your destinations on the map.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className='profile-travel-map'>
      <div className='profile-travel-map__header'>
        <div>
          <h2 className='profile-travel-map__title'>Your travel map</h2>
          <p className='profile-travel-map__description'>
            Explore the destinations from your trips.
          </p>
        </div>

        <span className='profile-travel-map__count'>
          {markers.length} {markers.length === 1 ? "location" : "locations"}
        </span>
      </div>

      <div className='profile-travel-map__container'>
        <MapContainer
          center={DEFAULT_CENTER}
          zoom={DEFAULT_ZOOM}
          scrollWheelZoom
          className='profile-travel-map__map'
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
          />

          <FitMapToMarkers coordinates={markerCoordinates} />

          {markers.map(({ trip, coordinates, key }) => (
            <Marker
              key={`${trip.tripId}-${key}`}
              position={[coordinates.latitude, coordinates.longitude]}
              icon={markerIcon}
            >
              <Popup>
                <div className='profile-travel-map__popup'>
                  <strong>{trip.title}</strong>
                  <p>{trip.destination}</p>
                  <a href={`/trips/${trip.tripId}/itinerary`}>View trip</a>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

      {isGeocoding && (
        <p className='profile-travel-map__status'>
          Finding your destinations...
        </p>
      )}

      {failedDestinations.length > 0 && (
        <p className='profile-travel-map__status'>
          Some destinations could not be located on the map.
        </p>
      )}
    </section>
  );
}
