package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public static final String COLLECTION_NAME = "orders";
    public static final String DEFAULT_CURRENCY = "INR";

    private final Firestore firestore;
    private final OfferService offerService;
    private final ProductListingService listingService;
    private final ProfileService profileService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback ONLY for isolated unit tests (when firestore is null)
    private final Map<String, Order> inMemoryOrders = new ConcurrentHashMap<>();

    @Autowired
    public OrderService(
            @Autowired(required = false) Firestore firestore,
            OfferService offerService,
            ProductListingService listingService,
            ProfileService profileService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.offerService = offerService;
        this.listingService = listingService;
        this.profileService = profileService;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Create an Order from an ACCEPTED offer with atomic Firestore transaction safety.
     */
    public synchronized OrderResponse createOrderFromOffer(String callerUid, String offerId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (offerId == null || offerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Offer ID is required.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference offerRef = firestore.collection(OfferService.COLLECTION_NAME).document(offerId);

                Order createdOrder = firestore.runTransaction(transaction -> {
                    DocumentSnapshot offerSnap = transaction.get(offerRef).get();
                    if (!offerSnap.exists()) {
                        throw new NoSuchElementException("Accepted offer not found with ID: " + offerId);
                    }

                    Offer offer = mapDocToOffer(offerSnap);

                    if (!OfferStatus.ACCEPTED.name().equalsIgnoreCase(offer.getStatus())) {
                        throw new IllegalStateException("An order can only be created from an ACCEPTED offer. Current offer status: " + offer.getStatus());
                    }

                    if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
                        throw new AccessDeniedException("Unauthorized to create order from this offer. Caller must be an authorized participant.");
                    }

                    // Check duplicate order for this offerId inside transaction
                    QuerySnapshot existingOrderSnaps = transaction.get(
                            firestore.collection(COLLECTION_NAME).whereEqualTo("offerId", offerId).limit(1)
                    ).get();

                    if (!existingOrderSnaps.isEmpty()) {
                        throw new IllegalStateException("An order has already been created from this accepted offer (Offer ID: " + offerId + ").");
                    }

                    DocumentReference listingRef = firestore.collection(ProductListingService.COLLECTION_NAME).document(offer.getListingId());
                    DocumentSnapshot listingSnap = transaction.get(listingRef).get();
                    if (!listingSnap.exists()) {
                        throw new NoSuchElementException("Marketplace listing not found with ID: " + offer.getListingId());
                    }

                    ProductListing listing = mapDocToListing(listingSnap);

                    Double agreedQuantity = offer.getAgreedQuantity() != null ? offer.getAgreedQuantity() : offer.getOfferedQuantity();
                    BigDecimal agreedPrice = offer.getAgreedPrice() != null ? offer.getAgreedPrice() : offer.getOfferedPrice();

                    if (agreedQuantity == null || agreedQuantity <= 0) {
                        throw new IllegalArgumentException("Invalid agreed quantity in accepted offer.");
                    }
                    if (agreedPrice == null || agreedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Invalid agreed price in accepted offer.");
                    }

                    Double currentAvailable = listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0.0;
                    if (currentAvailable < agreedQuantity) {
                        throw new IllegalArgumentException("Cannot create order. Requested quantity (" + agreedQuantity + ") exceeds available listing quantity (" + currentAvailable + ").");
                    }

                    BigDecimal lineTotal = agreedPrice.multiply(BigDecimal.valueOf(agreedQuantity)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal subtotal = lineTotal;
                    BigDecimal shippingCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal otherCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal totalAmount = subtotal.add(shippingCost).add(otherCost).setScale(2, RoundingMode.HALF_UP);

                    String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    String orderItemId = "item_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    String nowIso = Instant.now().toString();

                    OrderItem item = new OrderItem(
                            orderItemId,
                            orderId,
                            listing.getListingId(),
                            listing.getCropId(),
                            listing.getCropName(),
                            agreedQuantity,
                            offer.getQuantityUnit(),
                            agreedPrice,
                            offer.getPriceUnit(),
                            lineTotal
                    );

                    Order order = new Order(
                            orderId,
                            offer.getOfferId(),
                            listing.getListingId(),
                            offer.getFarmerId(),
                            offer.getBuyerUid(),
                            offer.getBuyerRole(),
                            listing.getCropId(),
                            listing.getCropName(),
                            OrderStatus.PENDING.name(),
                            agreedQuantity,
                            offer.getQuantityUnit(),
                            agreedPrice,
                            offer.getPriceUnit(),
                            subtotal,
                            shippingCost,
                            otherCost,
                            totalAmount,
                            DEFAULT_CURRENCY,
                            item,
                            null,
                            null,
                            nowIso,
                            nowIso,
                            null,
                            null,
                            null,
                            null
                    );

                    // Decrement listing available quantity
                    double newAvailable = currentAvailable - agreedQuantity;
                    listing.setAvailableQuantity(newAvailable);
                    if (newAvailable == 0.0 && ProductListingStatus.ACTIVE.name().equalsIgnoreCase(listing.getStatus())) {
                        listing.setStatus(ProductListingStatus.SOLD_OUT.name());
                    }
                    listing.setUpdatedAt(nowIso);

                    // Transactional writes
                    DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(orderId);
                    transaction.set(orderRef, mapOrderToDoc(order));
                    transaction.set(listingRef, mapListingToDoc(listing), SetOptions.merge());

                    return order;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(createdOrder, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to create order transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution for unit tests (when firestore is null)
        OfferResponse offerResp = offerService.getOfferById(offerId, callerUid);
        if (offerResp == null) {
            throw new NoSuchElementException("Accepted offer not found with ID: " + offerId);
        }
        if (!OfferStatus.ACCEPTED.name().equalsIgnoreCase(offerResp.getStatus())) {
            throw new IllegalStateException("An order can only be created from an ACCEPTED offer. Current offer status: " + offerResp.getStatus());
        }

        if (!callerUid.equals(offerResp.getBuyerUid()) && !callerUid.equals(offerResp.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to create order from this offer.");
        }

        boolean orderExistsInMemory = inMemoryOrders.values().stream().anyMatch(o -> offerId.equals(o.getOfferId()));
        if (orderExistsInMemory) {
            throw new IllegalStateException("An order has already been created from this accepted offer (Offer ID: " + offerId + ").");
        }

        ProductListingResponse listing = listingService.getListingById(offerResp.getListingId(), callerUid);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + offerResp.getListingId());
        }

        Double agreedQuantity = offerResp.getAgreedQuantity() != null ? offerResp.getAgreedQuantity() : offerResp.getOfferedQuantity();
        BigDecimal agreedPrice = offerResp.getAgreedPrice() != null ? offerResp.getAgreedPrice() : offerResp.getOfferedPrice();

        if (agreedQuantity == null || agreedQuantity <= 0) {
            throw new IllegalArgumentException("Invalid agreed quantity in accepted offer.");
        }
        if (agreedPrice == null || agreedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid agreed price in accepted offer.");
        }

        Double currentAvailable = listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0.0;
        if (currentAvailable < agreedQuantity) {
            throw new IllegalArgumentException("Cannot create order. Requested quantity (" + agreedQuantity + ") exceeds available listing quantity (" + currentAvailable + ").");
        }

        BigDecimal lineTotal = agreedPrice.multiply(BigDecimal.valueOf(agreedQuantity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = lineTotal;
        BigDecimal shippingCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(shippingCost).add(otherCost).setScale(2, RoundingMode.HALF_UP);

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String orderItemId = "item_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nowIso = Instant.now().toString();

        OrderItem item = new OrderItem(
                orderItemId,
                orderId,
                listing.getListingId(),
                listing.getCropId(),
                listing.getCropName(),
                agreedQuantity,
                offerResp.getQuantityUnit(),
                agreedPrice,
                offerResp.getPriceUnit(),
                lineTotal
        );

        Order order = new Order(
                orderId,
                offerResp.getOfferId(),
                listing.getListingId(),
                offerResp.getFarmerId(),
                offerResp.getBuyerUid(),
                offerResp.getBuyerRole(),
                listing.getCropId(),
                listing.getCropName(),
                OrderStatus.PENDING.name(),
                agreedQuantity,
                offerResp.getQuantityUnit(),
                agreedPrice,
                offerResp.getPriceUnit(),
                subtotal,
                shippingCost,
                otherCost,
                totalAmount,
                DEFAULT_CURRENCY,
                item,
                null,
                null,
                nowIso,
                nowIso,
                null,
                null,
                null,
                null
        );

        // Update listing availability in memory
        double newAvailable = currentAvailable - agreedQuantity;
        UpdateListingRequest ulr = new UpdateListingRequest();
        ulr.setAvailableQuantity(newAvailable);
        listingService.updateListing(offerResp.getFarmerId(), listing.getListingId(), ulr);

        inMemoryOrders.put(orderId, order);
        return toResponse(order, callerUid);
    }

    /**
     * Get single order details by ID.
     */
    public OrderResponse getOrderById(String orderId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Order order = fetchOrderFromFirestoreOrMemory(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to order.");
        }

        return toResponse(order, callerUid);
    }

    /**
     * Get orders initiated by buyer.
     */
    public OrderPageResponse getMyOrders(String buyerUid, String statusFilter, int page, int size) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Order> buyerOrders = fetchBuyerOrdersFromFirestoreOrMemory(buyerUid);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            buyerOrders = buyerOrders.stream()
                    .filter(o -> statusFilter.equalsIgnoreCase(o.getStatus()))
                    .collect(Collectors.toList());
        }

        buyerOrders.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = buyerOrders.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<OrderResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = buyerOrders.subList(fromIndex, toIndex).stream()
                    .map(o -> toResponse(o, buyerUid))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new OrderPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get orders received by farmer for owned listings.
     */
    public OrderPageResponse getReceivedOrders(String farmerId, String statusFilter, int page, int size) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Order> farmerOrders = fetchFarmerOrdersFromFirestoreOrMemory(farmerId);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            farmerOrders = farmerOrders.stream()
                    .filter(o -> statusFilter.equalsIgnoreCase(o.getStatus()))
                    .collect(Collectors.toList());
        }

        farmerOrders.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = farmerOrders.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<OrderResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = farmerOrders.subList(fromIndex, toIndex).stream()
                    .map(o -> toResponse(o, farmerId))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new OrderPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Confirm a PENDING order (Farmer only). State transition: PENDING -> CONFIRMED.
     */
    public synchronized OrderResponse confirmOrder(String farmerId, String orderId) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(orderId, farmerId, OrderStatus.PENDING, OrderStatus.CONFIRMED, (order, nowIso) -> {
            if (!farmerId.equals(order.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer can confirm this order.");
            }
            order.setConfirmedAt(nowIso);
        });
    }

    /**
     * Process a CONFIRMED order (Farmer only). State transition: CONFIRMED -> PROCESSING.
     */
    public synchronized OrderResponse processOrder(String farmerId, String orderId) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(orderId, farmerId, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, (order, nowIso) -> {
            if (!farmerId.equals(order.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer can move this order to processing.");
            }
            order.setProcessedAt(nowIso);
        });
    }

    /**
     * Complete a PROCESSING order (Farmer only). State transition: PROCESSING -> COMPLETED.
     */
    public synchronized OrderResponse completeOrder(String farmerId, String orderId) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(orderId, farmerId, OrderStatus.PROCESSING, OrderStatus.COMPLETED, (order, nowIso) -> {
            if (!farmerId.equals(order.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer can complete this order.");
            }
            order.setCompletedAt(nowIso);
        });
    }

    /**
     * Cancel an order in PENDING or CONFIRMED state with atomic inventory restoration.
     */
    public synchronized OrderResponse cancelOrder(String callerUid, String orderId, String reason) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(orderId);

                Order cancelledOrder = firestore.runTransaction(transaction -> {
                    DocumentSnapshot orderSnap = transaction.get(orderRef).get();
                    if (!orderSnap.exists()) {
                        throw new NoSuchElementException("Order not found with ID: " + orderId);
                    }

                    Order order = mapDocToOrder(orderSnap);

                    if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
                        throw new AccessDeniedException("Unauthorized to cancel order.");
                    }

                    String currentStatus = order.getStatus();
                    if (!OrderStatus.PENDING.name().equalsIgnoreCase(currentStatus) && !OrderStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)) {
                        throw new IllegalStateException("Cannot cancel order in status: " + currentStatus + ". Cancellation is only permitted for PENDING or CONFIRMED orders.");
                    }

                    String nowIso = Instant.now().toString();
                    order.setStatus(OrderStatus.CANCELLED.name());
                    order.setCancelledAt(nowIso);
                    order.setCancelledByUid(callerUid);
                    order.setCancellationReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by user");
                    order.setUpdatedAt(nowIso);

                    // Restore inventory atomically
                    DocumentReference listingRef = firestore.collection(ProductListingService.COLLECTION_NAME).document(order.getListingId());
                    DocumentSnapshot listingSnap = transaction.get(listingRef).get();
                    if (listingSnap.exists()) {
                        ProductListing listing = mapDocToListing(listingSnap);
                        Double currentAvailable = listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0.0;
                        double restoredAvailable = currentAvailable + order.getTotalQuantity();
                        listing.setAvailableQuantity(restoredAvailable);

                        if (ProductListingStatus.SOLD_OUT.name().equalsIgnoreCase(listing.getStatus()) && restoredAvailable > 0.0) {
                            listing.setStatus(ProductListingStatus.ACTIVE.name());
                        }
                        listing.setUpdatedAt(nowIso);
                        transaction.set(listingRef, mapListingToDoc(listing), SetOptions.merge());
                    }

                    transaction.set(orderRef, mapOrderToDoc(order));
                    return order;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(cancelledOrder, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to cancel order transaction: " + e.getMessage(), e);
            }
        }

        // In-memory fallback for unit tests
        Order order = inMemoryOrders.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to cancel order.");
        }

        String currentStatus = order.getStatus();
        if (!OrderStatus.PENDING.name().equalsIgnoreCase(currentStatus) && !OrderStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot cancel order in status: " + currentStatus + ". Cancellation is only permitted for PENDING or CONFIRMED orders.");
        }

        String nowIso = Instant.now().toString();
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelledAt(nowIso);
        order.setCancelledByUid(callerUid);
        order.setCancellationReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by user");
        order.setUpdatedAt(nowIso);

        // Restore inventory in-memory
        try {
            ProductListingResponse listing = listingService.getListingById(order.getListingId(), callerUid);
            if (listing != null) {
                Double currentAvailable = listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0.0;
                double restoredAvailable = currentAvailable + order.getTotalQuantity();
                UpdateListingRequest ulr = new UpdateListingRequest();
                ulr.setAvailableQuantity(restoredAvailable);
                listingService.updateListing(order.getFarmerId(), listing.getListingId(), ulr);

                if ("SOLD_OUT".equalsIgnoreCase(listing.getStatus()) && restoredAvailable > 0.0) {
                    UpdateListingStatusRequest ulsr = new UpdateListingStatusRequest();
                    ulsr.setStatus(ProductListingStatus.ACTIVE.name());
                    listingService.updateListingStatus(order.getFarmerId(), listing.getListingId(), ulsr);
                }
            }
        } catch (Exception ignored) {}

        inMemoryOrders.put(orderId, order);
        return toResponse(order, callerUid);
    }

    private interface StatusUpdater {
        void update(Order order, String nowIso);
    }

    private OrderResponse executeStatusTransition(String orderId, String callerUid, OrderStatus expectedStatus, OrderStatus targetStatus, StatusUpdater updater) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(orderId);

                Order updatedOrder = firestore.runTransaction(transaction -> {
                    DocumentSnapshot orderSnap = transaction.get(orderRef).get();
                    if (!orderSnap.exists()) {
                        throw new NoSuchElementException("Order not found with ID: " + orderId);
                    }

                    Order order = mapDocToOrder(orderSnap);

                    if (!expectedStatus.name().equalsIgnoreCase(order.getStatus())) {
                        throw new IllegalStateException("Cannot transition order from status " + order.getStatus() + " to " + targetStatus.name() + ". Expected status: " + expectedStatus.name());
                    }

                    String nowIso = Instant.now().toString();
                    updater.update(order, nowIso);
                    order.setStatus(targetStatus.name());
                    order.setUpdatedAt(nowIso);

                    transaction.set(orderRef, mapOrderToDoc(order));
                    return order;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(updatedOrder, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to update order status transaction: " + e.getMessage(), e);
            }
        }

        // In-memory fallback
        Order order = inMemoryOrders.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        if (!expectedStatus.name().equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Cannot transition order from status " + order.getStatus() + " to " + targetStatus.name() + ". Expected status: " + expectedStatus.name());
        }

        String nowIso = Instant.now().toString();
        updater.update(order, nowIso);
        order.setStatus(targetStatus.name());
        order.setUpdatedAt(nowIso);

        inMemoryOrders.put(orderId, order);
        return toResponse(order, callerUid);
    }

    private Order fetchOrderFromFirestoreOrMemory(String orderId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(orderId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                if (doc.exists()) {
                    return mapDocToOrder(doc);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching order {} from Firestore: {}", orderId, e.getMessage());
            }
        }
        return inMemoryOrders.get(orderId);
    }

    private List<Order> fetchBuyerOrdersFromFirestoreOrMemory(String buyerUid) {
        List<Order> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("buyerUid", buyerUid)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToOrder(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying buyer orders for {} from Firestore: {}", buyerUid, e.getMessage());
            }
        }

        return inMemoryOrders.values().stream()
                .filter(o -> buyerUid.equals(o.getBuyerUid()))
                .collect(Collectors.toList());
    }

    private List<Order> fetchFarmerOrdersFromFirestoreOrMemory(String farmerId) {
        List<Order> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("farmerId", farmerId)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToOrder(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying farmer orders for {} from Firestore: {}", farmerId, e.getMessage());
            }
        }

        return inMemoryOrders.values().stream()
                .filter(o -> farmerId.equals(o.getFarmerId()))
                .collect(Collectors.toList());
    }

    public OrderResponse toResponse(Order order, String callerUid) {
        if (order == null) return null;

        boolean isFarmer = callerUid != null && callerUid.equals(order.getFarmerId());
        boolean isBuyer = callerUid != null && callerUid.equals(order.getBuyerUid());

        String status = order.getStatus();
        boolean isPending = OrderStatus.PENDING.name().equalsIgnoreCase(status);
        boolean isConfirmed = OrderStatus.CONFIRMED.name().equalsIgnoreCase(status);
        boolean isProcessing = OrderStatus.PROCESSING.name().equalsIgnoreCase(status);

        boolean canConfirm = isFarmer && isPending;
        boolean canProcess = isFarmer && isConfirmed;
        boolean canComplete = isFarmer && isProcessing;
        boolean canCancel = (isFarmer || isBuyer) && (isPending || isConfirmed);

        OrderItemResponse itemResp = null;
        if (order.getItem() != null) {
            itemResp = new OrderItemResponse(
                    order.getItem().getOrderItemId(),
                    order.getItem().getOrderId(),
                    order.getItem().getListingId(),
                    order.getItem().getCropId(),
                    order.getItem().getCropName(),
                    order.getItem().getQuantity(),
                    order.getItem().getQuantityUnit(),
                    order.getItem().getAgreedUnitPrice(),
                    order.getItem().getPriceUnit(),
                    order.getItem().getLineTotal()
            );
        }

        return new OrderResponse(
                order.getOrderId(),
                order.getOfferId(),
                order.getListingId(),
                order.getFarmerId(),
                order.getBuyerUid(),
                order.getBuyerRole(),
                order.getCropId(),
                order.getCropName(),
                order.getStatus(),
                order.getTotalQuantity(),
                order.getQuantityUnit(),
                order.getAgreedPrice(),
                order.getPriceUnit(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getOtherCost(),
                order.getTotalAmount(),
                order.getCurrency(),
                itemResp,
                order.getCancellationReason(),
                order.getCancelledByUid(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt(),
                order.getProcessedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                canConfirm,
                canProcess,
                canComplete,
                canCancel
        );
    }

    private Order mapDocToOrder(DocumentSnapshot doc) {
        Order order = new Order();
        order.setOrderId(doc.getString("orderId"));
        order.setOfferId(doc.getString("offerId"));
        order.setListingId(doc.getString("listingId"));
        order.setFarmerId(doc.getString("farmerId"));
        order.setBuyerUid(doc.getString("buyerUid"));
        order.setBuyerRole(doc.getString("buyerRole"));
        order.setCropId(doc.getString("cropId"));
        order.setCropName(doc.getString("cropName"));
        order.setStatus(doc.getString("status"));
        order.setTotalQuantity(doc.getDouble("totalQuantity"));
        order.setQuantityUnit(doc.getString("quantityUnit"));

        if (doc.get("agreedPrice") != null) {
            order.setAgreedPrice(new BigDecimal(doc.get("agreedPrice").toString()));
        }
        order.setPriceUnit(doc.getString("priceUnit"));

        if (doc.get("subtotal") != null) order.setSubtotal(new BigDecimal(doc.get("subtotal").toString()));
        if (doc.get("shippingCost") != null) order.setShippingCost(new BigDecimal(doc.get("shippingCost").toString()));
        if (doc.get("otherCost") != null) order.setOtherCost(new BigDecimal(doc.get("otherCost").toString()));
        if (doc.get("totalAmount") != null) order.setTotalAmount(new BigDecimal(doc.get("totalAmount").toString()));

        order.setCurrency(doc.getString("currency"));
        order.setCancellationReason(doc.getString("cancellationReason"));
        order.setCancelledByUid(doc.getString("cancelledByUid"));

        order.setCreatedAt(doc.getString("createdAt"));
        order.setUpdatedAt(doc.getString("updatedAt"));
        order.setConfirmedAt(doc.getString("confirmedAt"));
        order.setProcessedAt(doc.getString("processedAt"));
        order.setCompletedAt(doc.getString("completedAt"));
        order.setCancelledAt(doc.getString("cancelledAt"));

        Map<String, Object> itemMap = (Map<String, Object>) doc.get("item");
        if (itemMap != null) {
            OrderItem item = new OrderItem();
            item.setOrderItemId((String) itemMap.get("orderItemId"));
            item.setOrderId((String) itemMap.get("orderId"));
            item.setListingId((String) itemMap.get("listingId"));
            item.setCropId((String) itemMap.get("cropId"));
            item.setCropName((String) itemMap.get("cropName"));
            if (itemMap.get("quantity") != null) item.setQuantity(((Number) itemMap.get("quantity")).doubleValue());
            item.setQuantityUnit((String) itemMap.get("quantityUnit"));
            if (itemMap.get("agreedUnitPrice") != null) item.setAgreedUnitPrice(new BigDecimal(itemMap.get("agreedUnitPrice").toString()));
            item.setPriceUnit((String) itemMap.get("priceUnit"));
            if (itemMap.get("lineTotal") != null) item.setLineTotal(new BigDecimal(itemMap.get("lineTotal").toString()));
            order.setItem(item);
        }

        return order;
    }

    private Map<String, Object> mapOrderToDoc(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("offerId", order.getOfferId());
        map.put("listingId", order.getListingId());
        map.put("farmerId", order.getFarmerId());
        map.put("buyerUid", order.getBuyerUid());
        map.put("buyerRole", order.getBuyerRole());
        map.put("cropId", order.getCropId());
        map.put("cropName", order.getCropName());
        map.put("status", order.getStatus());
        map.put("totalQuantity", order.getTotalQuantity());
        map.put("quantityUnit", order.getQuantityUnit());
        map.put("agreedPrice", order.getAgreedPrice() != null ? order.getAgreedPrice().toString() : null);
        map.put("priceUnit", order.getPriceUnit());
        map.put("subtotal", order.getSubtotal() != null ? order.getSubtotal().toString() : null);
        map.put("shippingCost", order.getShippingCost() != null ? order.getShippingCost().toString() : "0.00");
        map.put("otherCost", order.getOtherCost() != null ? order.getOtherCost().toString() : "0.00");
        map.put("totalAmount", order.getTotalAmount() != null ? order.getTotalAmount().toString() : null);
        map.put("currency", order.getCurrency());
        map.put("cancellationReason", order.getCancellationReason());
        map.put("cancelledByUid", order.getCancelledByUid());
        map.put("createdAt", order.getCreatedAt());
        map.put("updatedAt", order.getUpdatedAt());
        map.put("confirmedAt", order.getConfirmedAt());
        map.put("processedAt", order.getProcessedAt());
        map.put("completedAt", order.getCompletedAt());
        map.put("cancelledAt", order.getCancelledAt());

        if (order.getItem() != null) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("orderItemId", order.getItem().getOrderItemId());
            itemMap.put("orderId", order.getItem().getOrderId());
            itemMap.put("listingId", order.getItem().getListingId());
            itemMap.put("cropId", order.getItem().getCropId());
            itemMap.put("cropName", order.getItem().getCropName());
            itemMap.put("quantity", order.getItem().getQuantity());
            itemMap.put("quantityUnit", order.getItem().getQuantityUnit());
            itemMap.put("agreedUnitPrice", order.getItem().getAgreedUnitPrice() != null ? order.getItem().getAgreedUnitPrice().toString() : null);
            itemMap.put("priceUnit", order.getItem().getPriceUnit());
            itemMap.put("lineTotal", order.getItem().getLineTotal() != null ? order.getItem().getLineTotal().toString() : null);
            map.put("item", itemMap);
        }

        return map;
    }

    private Offer mapDocToOffer(DocumentSnapshot doc) {
        Offer offer = new Offer();
        offer.setOfferId(doc.getString("offerId"));
        offer.setListingId(doc.getString("listingId"));
        offer.setFarmerId(doc.getString("farmerId"));
        offer.setBuyerUid(doc.getString("buyerUid"));
        offer.setBuyerRole(doc.getString("buyerRole"));
        offer.setCropId(doc.getString("cropId"));
        offer.setCropName(doc.getString("cropName"));
        offer.setOfferedQuantity(doc.getDouble("offeredQuantity"));
        offer.setQuantityUnit(doc.getString("quantityUnit"));

        if (doc.get("offeredPrice") != null) {
            offer.setOfferedPrice(new BigDecimal(doc.get("offeredPrice").toString()));
        }
        offer.setPriceUnit(doc.getString("priceUnit"));
        offer.setStatus(doc.getString("status"));

        if (doc.get("agreedQuantity") != null) {
            offer.setAgreedQuantity(doc.getDouble("agreedQuantity"));
        }
        if (doc.get("agreedPrice") != null) {
            offer.setAgreedPrice(new BigDecimal(doc.get("agreedPrice").toString()));
        }
        offer.setAgreedPriceUnit(doc.getString("agreedPriceUnit"));
        return offer;
    }

    private ProductListing mapDocToListing(DocumentSnapshot doc) {
        ProductListing listing = new ProductListing();
        listing.setListingId(doc.getString("listingId"));
        listing.setOwnerUid(doc.getString("ownerUid"));
        listing.setFarmerProfileId(doc.getString("farmerProfileId"));
        listing.setCropId(doc.getString("cropId"));
        listing.setCropName(doc.getString("cropName"));
        listing.setQuantity(doc.getDouble("quantity"));
        listing.setAvailableQuantity(doc.getDouble("availableQuantity"));
        listing.setUnit(doc.getString("unit"));
        if (doc.get("askingPrice") != null) {
            listing.setAskingPrice(doc.getDouble("askingPrice"));
        }
        listing.setPriceUnit(doc.getString("priceUnit"));
        listing.setStatus(doc.getString("status"));
        return listing;
    }

    private Map<String, Object> mapListingToDoc(ProductListing listing) {
        Map<String, Object> map = new HashMap<>();
        map.put("availableQuantity", listing.getAvailableQuantity());
        map.put("status", listing.getStatus());
        map.put("updatedAt", listing.getUpdatedAt() != null ? listing.getUpdatedAt() : Instant.now().toString());
        return map;
    }
}
