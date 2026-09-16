import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import type { NotificationListResponse, UnreadCountResponse, NotificationItem } from '../types/notification';
import { useAuth } from '../features/auth/hooks/useAuth';

export const NOTIFICATIONS_QUERY_KEY = 'notifications';

export function useNotifications(params?: { unreadOnly?: boolean; page?: number; pageSize?: number }) {
  const { isAuthenticated } = useAuth();
  const unreadOnly = params?.unreadOnly ?? false;
  const page = params?.page ?? 0;
  const pageSize = params?.pageSize ?? 20;

  return useQuery<NotificationListResponse>({
    queryKey: [NOTIFICATIONS_QUERY_KEY, 'list', { unreadOnly, page, pageSize }],
    queryFn: () => notificationService.getNotifications({ unreadOnly, page, pageSize }),
    enabled: isAuthenticated,
    staleTime: 30000,
    gcTime: 300000,
  });
}

export function useUnreadNotifications(params?: { page?: number; pageSize?: number }) {
  const { isAuthenticated } = useAuth();
  const page = params?.page ?? 0;
  const pageSize = params?.pageSize ?? 20;

  return useQuery<NotificationListResponse>({
    queryKey: [NOTIFICATIONS_QUERY_KEY, 'unread', { page, pageSize }],
    queryFn: () => notificationService.getUnreadNotifications({ page, pageSize }),
    enabled: isAuthenticated,
    staleTime: 30000,
    gcTime: 300000,
  });
}

export function useUnreadNotificationCount() {
  const { isAuthenticated } = useAuth();

  return useQuery<UnreadCountResponse>({
    queryKey: [NOTIFICATIONS_QUERY_KEY, 'unread-count'],
    queryFn: () => notificationService.getUnreadCount(),
    enabled: isAuthenticated,
    staleTime: 15000,
    refetchOnWindowFocus: true,
  });
}

export function useNotification(notificationId: string) {
  const { isAuthenticated } = useAuth();

  return useQuery<NotificationItem>({
    queryKey: [NOTIFICATIONS_QUERY_KEY, 'detail', notificationId],
    queryFn: () => notificationService.getNotificationById(notificationId),
    enabled: isAuthenticated && Boolean(notificationId),
    staleTime: 30000,
  });
}

export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => notificationService.markAsRead(notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] });
    },
  });
}

export function useMarkAllNotificationsAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] });
    },
  });
}

export function useDeleteNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => notificationService.deleteNotification(notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [NOTIFICATIONS_QUERY_KEY] });
    },
  });
}
