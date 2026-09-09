import type { CityOption } from "./CityOption";

export type CitySelectProps = {
  value: CityOption | null;
  onChange: (city: CityOption | null) => void;
};
