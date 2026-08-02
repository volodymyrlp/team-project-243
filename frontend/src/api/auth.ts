const API_URL = import.meta.env.VITE_API_URL;

interface RegistrationData {
  fullName: string;
  email: string;
  passwordHash: string;
  confirmPassword: string;
}

export const registerUser = async (data: RegistrationData) => {
  const response = await fetch(`${API_URL}/api/auth/registration`, {
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