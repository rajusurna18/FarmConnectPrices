export interface CreateReviewRequest {
  orderId: string;
  rating: number; // 1 to 5
  title?: string;
  comment: string;
}

export interface ReviewResponse {
  reviewId: string;
  orderId: string;
  listingId: string;
  cropId: string;
  cropName: string;
  reviewerUid: string;
  reviewerRole: string;
  reviewerDisplayName: string;
  revieweeUid: string;
  revieweeRole: string;
  revieweeDisplayName: string;
  rating: number;
  title: string;
  comment: string;
  status: string;
  verifiedTransaction: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewEligibilityResponse {
  orderId: string;
  eligible: boolean;
  reason: string;
  revieweeUid: string | null;
  revieweeDisplayName: string | null;
  revieweeRole: string | null;
  alreadyReviewed: boolean;
}

export interface RatingSummaryResponse {
  userId: string;
  userRole: string | null;
  averageRating: number;
  totalReviews: number;
  totalRatingPoints: number;
  oneStarCount: number;
  twoStarCount: number;
  threeStarCount: number;
  fourStarCount: number;
  fiveStarCount: number;
  updatedAt: string;
}

export interface ReviewPageResponse {
  items: ReviewResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
