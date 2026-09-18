export interface NotificationPreference {
  offers: boolean;
  orders: boolean;
  payments: boolean;
  delivery: boolean;
  reviews: boolean;
}

export interface UpdateNotificationPreferenceRequest {
  offers?: boolean;
  orders?: boolean;
  payments?: boolean;
  delivery?: boolean;
  reviews?: boolean;
}

export interface CategoryMetadata {
  key: keyof NotificationPreference;
  title: string;
  description: string;
  iconName: 'Tag' | 'ShoppingBag' | 'CreditCard' | 'Truck' | 'Star';
}

export const NOTIFICATION_CATEGORIES: CategoryMetadata[] = [
  {
    key: 'offers',
    title: 'Offers & Negotiation',
    description: 'Get notified about new offers, counter offers, acceptance, or rejection decisions.',
    iconName: 'Tag',
  },
  {
    key: 'orders',
    title: 'Orders & Fulfillment',
    description: 'Get notified about order placement, confirmation, processing, and completion status.',
    iconName: 'ShoppingBag',
  },
  {
    key: 'payments',
    title: 'Payments & Transactions',
    description: 'Get notified about payment creation, successful payment releases, or transaction failures.',
    iconName: 'CreditCard',
  },
  {
    key: 'delivery',
    title: 'Delivery & Logistics',
    description: 'Get notified when produce is picked up, in transit, out for delivery, or delivered.',
    iconName: 'Truck',
  },
  {
    key: 'reviews',
    title: 'Reviews & Trust Ratings',
    description: 'Get notified when commercial trading partners leave verified feedback or reviews.',
    iconName: 'Star',
  },
];
