import type { CityOption } from "./CityOption";

export interface TripForm {
  name: string;
  destination: CityOption | null;
  startDate: Date | null;
  endDate: Date | null;
  description: string;
  budget: string;
  currency: string;
  isPublic: boolean;
}