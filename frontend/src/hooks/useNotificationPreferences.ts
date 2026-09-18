import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationPreferenceService } from '../services/notificationPreferenceService';
import type {
  NotificationPreference,
  UpdateNotificationPreferenceRequest,
} from '../types/notificationPreference';

export const NOTIFICATION_PREFERENCES_QUERY_KEY = ['notification-preferences'];

/**
 * Hook to fetch current user's notification preferences.
 */
export function useNotificationPreferences() {
  return useQuery<NotificationPreference, Error>({
    queryKey: NOTIFICATION_PREFERENCES_QUERY_KEY,
    queryFn: () => notificationPreferenceService.getNotificationPreferences(),
    staleTime: 5 * 60 * 1000, // 5 minutes cache
  });
}

/**
 * Hook to update user's notification preferences.
 */
export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient();

  return useMutation<NotificationPreference, Error, UpdateNotificationPreferenceRequest>({
    mutationFn: (request) => notificationPreferenceService.updateNotificationPreferences(request),
    onSuccess: (updatedData) => {
      queryClient.setQueryData(NOTIFICATION_PREFERENCES_QUERY_KEY, updatedData);
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_PREFERENCES_QUERY_KEY });
    },
  });
}
