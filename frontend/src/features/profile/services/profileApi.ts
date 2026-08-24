import { apiClient } from '../../../services/api';
import type { Profile, UpdateProfilePayload, RoleSelectionPayload } from '../types';

export const fetchProfile = async (): Promise<Profile> => {
  const response = await apiClient.get<Profile>('/api/v1/profile');
  return response.data;
};

export const updateProfile = async (payload: UpdateProfilePayload): Promise<Profile> => {
  const response = await apiClient.put<Profile>('/api/v1/profile', payload);
  return response.data;
};

export const selectRole = async (payload: RoleSelectionPayload): Promise<Profile> => {
  const response = await apiClient.put<Profile>('/api/v1/profile/role', payload);
  return response.data;
};
