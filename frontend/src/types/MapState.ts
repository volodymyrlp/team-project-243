import type { CityCoordinates } from "./CityCoordinates";

export type MapState = {
  coordinates: CityCoordinates | null;
  isLoading: boolean;
  error: string;
  destination: string;
};
