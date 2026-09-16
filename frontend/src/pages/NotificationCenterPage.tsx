import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Navbar } from '../components/navigation/Navbar';
import { Footer } from '../components/navigation/Footer';
import {
  useNotifications,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
} from '../hooks/useNotifications';
import type { NotificationItem, NotificationType } from '../types/notification';
import {
  Bell,
  CheckCheck,
  Tag,
  ShoppingBag,
  CreditCard,
  Truck,
  Star,
  Trash2,
  Filter,
  RefreshCw,
  Inbox,
  AlertCircle,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowRight,
} from 'lucide-react';

function formatRelativeTime(dateString: string): string {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (seconds < 60) return 'Just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined });
}

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'NEW_OFFER':
    case 'COUNTER_OFFER_RECEIVED':
      return <Tag className="w-5 h-5 text-amber-400" />;
    case 'OFFER_ACCEPTED':
      return <CheckCircle2 className="w-5 h-5 text-emerald-400" />;
    case 'OFFER_REJECTED':
    case 'OFFER_CANCELLED':
      return <XCircle className="w-5 h-5 text-rose-400" />;

    case 'ORDER_CREATED':
    case 'ORDER_CONFIRMED':
    case 'ORDER_PROCESSING':
    case 'ORDER_COMPLETED':
      return <ShoppingBag className="w-5 h-5 text-emerald-400" />;
    case 'ORDER_CANCELLED':
      return <XCircle className="w-5 h-5 text-rose-400" />;

    case 'PAYMENT_SUCCESS':
      return <CreditCard className="w-5 h-5 text-emerald-400" />;
    case 'PAYMENT_FAILED':
      return <AlertCircle className="w-5 h-5 text-rose-400" />;
    case 'PAYMENT_CREATED':
      return <CreditCard className="w-5 h-5 text-blue-400" />;

    case 'DELIVERY_ASSIGNED':
    case 'DELIVERY_READY_FOR_PICKUP':
    case 'DELIVERY_PICKED_UP':
    case 'DELIVERY_IN_TRANSIT':
    case 'DELIVERY_OUT_FOR_DELIVERY':
    case 'DELIVERY_DELIVERED':
      return <Truck className="w-5 h-5 text-blue-400" />;
    case 'DELIVERY_CANCELLED':
      return <Truck className="w-5 h-5 text-rose-400" />;

    case 'NEW_REVIEW':
      return <Star className="w-5 h-5 text-yellow-400" />;

    default:
      return <Bell className="w-5 h-5 text-emerald-400" />;
  }
}

export const NotificationCenterPage: React.FC = () => {
  const navigate = useNavigate();
  const [unreadOnlyFilter, setUnreadOnlyFilter] = useState(false);
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const { data, isLoading, isError, error, refetch } = useNotifications({
    unreadOnly: unreadOnlyFilter,
    page,
    pageSize,
  });

  const markAsReadMutation = useMarkNotificationAsRead();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteMutation = useDeleteNotification();

  const notifications = data?.notifications || [];
  const unreadCount = data?.unreadCount || 0;
  const totalCount = data?.totalCount || 0;

  const handleNotificationClick = (item: NotificationItem) => {
    if (!item.read) {
      markAsReadMutation.mutate(item.notificationId);
    }
    if (item.relatedRoute) {
      navigate(item.relatedRoute);
    }
  };

  const handleMarkAsRead = (e: React.MouseEvent, item: NotificationItem) => {
    e.stopPropagation();
    if (!item.read) {
      markAsReadMutation.mutate(item.notificationId);
    }
  };

  const handleDelete = (e: React.MouseEvent, notificationId: string) => {
    e.stopPropagation();
    deleteMutation.mutate(notificationId);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between">
      <Navbar />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 w-full flex-grow">
        {/* Header Section */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center space-x-3 mb-1">
              <div className="p-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Bell className="w-6 h-6" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Notification Center
              </h1>
              {unreadCount > 0 && (
                <span className="px-2.5 py-0.5 text-xs font-bold bg-emerald-500 text-slate-950 rounded-full">
                  {unreadCount} unread
                </span>
              )}
            </div>
            <p className="text-slate-400 text-sm">
              Stay updated on marketplace offers, orders, payments, deliveries, and reviews.
            </p>
          </div>

          {/* Action CTAs */}
          <div className="flex items-center space-x-3">
            <button
              onClick={() => markAllAsReadMutation.mutate()}
              disabled={unreadCount === 0 || markAllAsReadMutation.isPending}
              className="px-4 py-2.5 text-xs sm:text-sm font-semibold rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700/60 text-slate-200 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center space-x-2 shadow-sm"
              title="Mark all notifications as read"
            >
              <CheckCheck className="w-4 h-4 text-emerald-400" />
              <span>Mark all read</span>
            </button>

            <button
              onClick={() => refetch()}
              className="p-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700/60 text-slate-400 hover:text-slate-200 transition-colors"
              title="Refresh notifications"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-4 mb-6">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                setUnreadOnlyFilter(false);
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-medium transition-all ${
                !unreadOnlyFilter
                  ? 'bg-emerald-600 text-white font-semibold shadow-lg shadow-emerald-950/40'
                  : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              All Notifications ({totalCount})
            </button>

            <button
              onClick={() => {
                setUnreadOnlyFilter(true);
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-medium transition-all ${
                unreadOnlyFilter
                  ? 'bg-emerald-600 text-white font-semibold shadow-lg shadow-emerald-950/40'
                  : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              Unread Only ({unreadCount})
            </button>
          </div>

          <div className="hidden sm:flex items-center text-xs text-slate-400 space-x-1">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <span>Showing derived events</span>
          </div>
        </div>

        {/* Loading State Skeleton */}
        {isLoading && (
          <div className="space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/60 animate-pulse flex items-start space-x-4"
              >
                <div className="w-10 h-10 rounded-xl bg-slate-800 shrink-0" />
                <div className="flex-grow space-y-2">
                  <div className="h-4 bg-slate-800 rounded w-1/3" />
                  <div className="h-3.5 bg-slate-800/70 rounded w-3/4" />
                  <div className="h-3 bg-slate-800/40 rounded w-1/5" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="p-8 rounded-2xl bg-slate-900/80 border border-rose-500/20 text-center space-y-4 my-6">
            <AlertCircle className="w-10 h-10 text-rose-400 mx-auto" />
            <div>
              <h3 className="text-lg font-bold text-white">Failed to load notifications</h3>
              <p className="text-slate-400 text-sm mt-1">
                {(error as Error)?.message || 'An unexpected error occurred.'}
              </p>
            </div>
            <button
              onClick={() => refetch()}
              className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-semibold rounded-xl text-xs transition-colors"
            >
              Try Again
            </button>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !isError && notifications.length === 0 && (
          <div className="p-12 rounded-3xl bg-slate-900/40 border border-slate-800/80 text-center space-y-4 my-6">
            <div className="w-16 h-16 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-500 mx-auto">
              <Inbox className="w-8 h-8" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">
                {unreadOnlyFilter ? 'No unread notifications' : "You're all caught up!"}
              </h3>
              <p className="text-slate-400 text-sm mt-1 max-w-sm mx-auto">
                {unreadOnlyFilter
                  ? 'There are no unread notifications right now.'
                  : 'Important marketplace updates for offers, orders, payments, and deliveries will appear here.'}
              </p>
            </div>
            {unreadOnlyFilter && (
              <button
                onClick={() => setUnreadOnlyFilter(false)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-xs transition-colors"
              >
                View all notifications
              </button>
            )}
          </div>
        )}

        {/* Notification List */}
        {!isLoading && !isError && notifications.length > 0 && (
          <div className="space-y-3">
            {notifications.map((item) => (
              <div
                key={item.notificationId}
                onClick={() => handleNotificationClick(item)}
                className={`p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer group flex items-start space-x-4 relative ${
                  !item.read
                    ? 'bg-slate-900/90 border-emerald-500/40 shadow-lg shadow-slate-950/50 hover:border-emerald-400/60'
                    : 'bg-slate-900/40 border-slate-800/60 hover:bg-slate-900/70 hover:border-slate-700'
                }`}
              >
                {/* Unread Indicator Accent */}
                {!item.read && (
                  <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-emerald-400 rounded-r-full" />
                )}

                {/* Icon Container */}
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 shrink-0 mt-0.5 group-hover:scale-105 transition-transform">
                  {getNotificationIcon(item.type)}
                </div>

                {/* Content */}
                <div className="flex-grow min-w-0">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <h4 className={`text-sm sm:text-base font-bold truncate ${!item.read ? 'text-white' : 'text-slate-300'}`}>
                      {item.title}
                    </h4>
                    <span className="text-xs text-slate-400 shrink-0 flex items-center space-x-1">
                      <Clock className="w-3 h-3 text-slate-500" />
                      <span>{formatRelativeTime(item.createdAt)}</span>
                    </span>
                  </div>

                  <p className="text-xs sm:text-sm text-slate-300 leading-relaxed mb-3">
                    {item.message}
                  </p>

                  <div className="flex items-center justify-between">
                    {item.relatedRoute ? (
                      <span className="text-xs font-semibold text-emerald-400 group-hover:text-emerald-300 flex items-center space-x-1">
                        <span>View details</span>
                        <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
                      </span>
                    ) : (
                      <span />
                    )}

                    {/* Actions */}
                    <div className="flex items-center space-x-2">
                      {!item.read && (
                        <button
                          onClick={(e) => handleMarkAsRead(e, item)}
                          className="px-2.5 py-1 text-xs font-medium text-emerald-400 hover:text-emerald-300 hover:bg-emerald-500/10 rounded-lg transition-colors"
                          title="Mark as read"
                        >
                          Mark read
                        </button>
                      )}
                      <button
                        onClick={(e) => handleDelete(e, item.notificationId)}
                        className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors"
                        title="Delete notification"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination Control */}
        {data && (data.hasMore || page > 0) && (
          <div className="flex items-center justify-between mt-8 pt-4 border-t border-slate-800">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Previous
            </button>
            <span className="text-xs text-slate-400">Page {page + 1}</span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={!data.hasMore}
              className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Next
            </button>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
