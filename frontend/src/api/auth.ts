import type { LoginData } from "../types/LoginData";
import type { LoginResponse } from "../types/LoginResponse";
import type { RegistrationData } from "../types/RegistrationData";


const API_URL = import.meta.env.VITE_API_URL;

export const registerUser = async (data: RegistrationData) => {
  const response = await fetch(`${API_URL}/api/v1/auth/registration`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || "Registration failed");
  }

  return text ? JSON.parse(text) : null;
};

export const loginUser = async (data: LoginData): Promise<LoginResponse> => {
  const response = await fetch(`${API_URL}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || "Login failed");
  }

  return JSON.parse(text);
};
