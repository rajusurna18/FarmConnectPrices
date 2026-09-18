import { apiClient } from './api';
import type {
  NotificationPreference,
  UpdateNotificationPreferenceRequest,
} from '../types/notificationPreference';

export const notificationPreferenceService = {
  /**
   * Fetch authenticated user's notification preferences.
   */
  async getNotificationPreferences(): Promise<NotificationPreference> {
    const response = await apiClient.get<NotificationPreference>('/api/v1/notification-preferences');
    return response.data;
  },

  /**
   * Update authenticated user's notification preferences.
   */
  async updateNotificationPreferences(
    request: UpdateNotificationPreferenceRequest
  ): Promise<NotificationPreference> {
    const response = await apiClient.put<NotificationPreference>(
      '/api/v1/notification-preferences',
      request
    );
    return response.data;
  },
};
