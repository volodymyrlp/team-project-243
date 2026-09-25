export interface TripResponse {
  tripId: string;
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  description: string | null;
  budget: number | null;
  currency: string | null;
  coverUrl: string | null;
  isPublic: boolean;
  createdAt: string;
}