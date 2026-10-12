export type GeocodingResponse = {
  results?: Array<{
    latitude: number;
    longitude: number;
    name: string;
    country?: string;
  }>;
};
