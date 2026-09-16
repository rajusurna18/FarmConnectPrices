import { apiClient } from './api';
import type {
  NotificationItem,
  NotificationListResponse,
  UnreadCountResponse,
} from '../types/notification';

export const notificationService = {
  /**
   * Fetch authenticated user's notifications with pagination and unread filter.
   */
  async getNotifications(params?: {
    unreadOnly?: boolean;
    page?: number;
    pageSize?: number;
  }): Promise<NotificationListResponse> {
    const response = await apiClient.get<NotificationListResponse>('/api/v1/notifications', {
      params: {
        unreadOnly: params?.unreadOnly ?? false,
        page: params?.page ?? 0,
        pageSize: params?.pageSize ?? 20,
      },
    });
    return response.data;
  },

  /**
   * Fetch authenticated user's unread notifications.
   */
  async getUnreadNotifications(params?: { page?: number; pageSize?: number }): Promise<NotificationListResponse> {
    const response = await apiClient.get<NotificationListResponse>('/api/v1/notifications/unread', {
      params: {
        page: params?.page ?? 0,
        pageSize: params?.pageSize ?? 20,
      },
    });
    return response.data;
  },

  /**
   * Fetch authenticated user's unread notification count.
   */
  async getUnreadCount(): Promise<UnreadCountResponse> {
    const response = await apiClient.get<UnreadCountResponse>('/api/v1/notifications/unread-count');
    return response.data;
  },

  /**
   * Fetch single notification by ID.
   */
  async getNotificationById(notificationId: string): Promise<NotificationItem> {
    const response = await apiClient.get<NotificationItem>(`/api/v1/notifications/${notificationId}`);
    return response.data;
  },

  /**
   * Mark single notification as read.
   */
  async markAsRead(notificationId: string): Promise<NotificationItem> {
    const response = await apiClient.patch<NotificationItem>(`/api/v1/notifications/${notificationId}/read`);
    return response.data;
  },

  /**
   * Mark all notifications as read for current user.
   */
  async markAllAsRead(): Promise<UnreadCountResponse> {
    const response = await apiClient.patch<UnreadCountResponse>('/api/v1/notifications/read-all');
    return response.data;
  },

  /**
   * Delete a single notification.
   */
  async deleteNotification(notificationId: string): Promise<void> {
    await apiClient.delete(`/api/v1/notifications/${notificationId}`);
  },
};
