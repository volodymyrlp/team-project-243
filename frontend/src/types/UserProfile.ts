export interface UserProfile {
  userId: string;
  email: string;
  fullName: string;
  createdAt: string;
  avatarUrl: string | null;
  trips: unknown[];
}