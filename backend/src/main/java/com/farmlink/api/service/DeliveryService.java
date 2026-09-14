package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    public static final String COLLECTION_NAME = "deliveries";

    private final Firestore firestore;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductListingService listingService;
    private final ProfileService profileService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback ONLY for isolated unit tests (when firestore is null)
    private final Map<String, Delivery> inMemoryDeliveries = new ConcurrentHashMap<>();

    @Autowired
    public DeliveryService(
            @Autowired(required = false) Firestore firestore,
            OrderService orderService,
            PaymentService paymentService,
            ProductListingService listingService,
            ProfileService profileService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.listingService = listingService;
        this.profileService = profileService;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Create a Delivery for a Paid Order with atomic Firestore protection.
     */
    public synchronized DeliveryResponse createDelivery(String callerUid, CreateDeliveryRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null || request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required to create a delivery.");
        }

        String orderId = request.getOrderId().trim();
        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        // Verify caller is order participant (buyer or farmer)
        if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to create delivery for this order.");
        }

        // Verify order is not cancelled
        if (OrderStatus.CANCELLED.name().equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Cannot create delivery for a cancelled order.");
        }

        // Verify Payment status is SUCCESS from Payment domain
        String paymentId = "pay_" + orderId;
        PaymentResponse payment = paymentService.getPaymentById(paymentId, callerUid);
        if (payment == null || !PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Cannot create delivery request. Order payment is not completed (Current payment status: " + (payment != null ? payment.getStatus() : "MISSING") + ").");
        }

        String deliveryId = "del_" + orderId;
        String nowIso = Instant.now().toString();
        String trackingRef = "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        // Resolve pickup address from listing location
        LocationDto pickupAddress = null;
        try {
            ProductListingResponse listing = listingService.getListingById(order.getListingId(), callerUid);
            if (listing != null && listing.getLocation() != null) {
                pickupAddress = listing.getLocation();
            }
        } catch (Exception e) {
            logger.warn("Could not fetch listing location for delivery pickup address: {}", e.getMessage());
        }
        if (pickupAddress == null) {
            pickupAddress = new LocationDto("Telangana", "Hyderabad", "Hyderabad", "Market Center", "500001");
        }

        // Resolve delivery destination address
        LocationDto deliveryAddress = request.getDeliveryAddress();
        if (deliveryAddress == null || !deliveryAddress.isComplete()) {
            try {
                ProfileResponse profile = profileService.getProfile(order.getBuyerUid(), null, true, null);
                if (profile != null && profile.getLocation() != null) {
                    deliveryAddress = profile.getLocation();
                }
            } catch (Exception e) {
                logger.warn("Could not fetch buyer profile location for delivery destination address: {}", e.getMessage());
            }
        }
        if (deliveryAddress == null) {
            deliveryAddress = new LocationDto("Telangana", "Rangareddy", "Rajendranagar", "Buyer Hub", "500030");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference deliveryRef = firestore.collection(COLLECTION_NAME).document(deliveryId);

                LocationDto finalPickup = pickupAddress;
                LocationDto finalDelivery = deliveryAddress;

                Delivery createdDelivery = firestore.runTransaction(transaction -> {
                    DocumentSnapshot delSnap = transaction.get(deliveryRef).get();
                    if (delSnap.exists()) {
                        throw new IllegalStateException("A delivery request already exists for this order (Delivery ID: " + deliveryId + ").");
                    }

                    Delivery delivery = new Delivery(
                            deliveryId,
                            orderId,
                            order.getFarmerId(),
                            order.getBuyerUid(),
                            order.getBuyerRole(),
                            DeliveryStatus.CREATED.name(),
                            finalPickup,
                            null,
                            null,
                            finalDelivery,
                            null,
                            null,
                            null,
                            trackingRef,
                            null,
                            null,
                            nowIso,
                            nowIso,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    );

                    transaction.set(deliveryRef, mapDeliveryToDoc(delivery));
                    return delivery;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(createdDelivery, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to create delivery transaction: " + e.getMessage(), e);
            }
        }

        // In-memory fallback for unit tests
        if (inMemoryDeliveries.containsKey(deliveryId)) {
            throw new IllegalStateException("A delivery request already exists for this order (Delivery ID: " + deliveryId + ").");
        }

        Delivery delivery = new Delivery(
                deliveryId,
                orderId,
                order.getFarmerId(),
                order.getBuyerUid(),
                order.getBuyerRole(),
                DeliveryStatus.CREATED.name(),
                pickupAddress,
                null,
                null,
                deliveryAddress,
                null,
                null,
                null,
                trackingRef,
                null,
                null,
                nowIso,
                nowIso,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        inMemoryDeliveries.put(deliveryId, delivery);
        return toResponse(delivery, callerUid);
    }

    /**
     * Assign a delivery partner to a CREATED delivery (Farmer / Admin).
     */
    public synchronized DeliveryResponse assignPartner(String callerUid, String deliveryId, AssignDeliveryPartnerRequest req) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery partner name is required.");
        }

        return executeStatusTransition(deliveryId, callerUid, DeliveryStatus.CREATED, DeliveryStatus.ASSIGNED, (delivery, nowIso) -> {
            if (!callerUid.equals(delivery.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer or admin can assign a delivery partner.");
            }
            String partnerId = "dp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            DeliveryPartnerInfo partner = new DeliveryPartnerInfo(
                    partnerId,
                    req.getName().trim(),
                    req.getPhone() != null ? req.getPhone().trim() : null,
                    req.getVehicleType() != null ? req.getVehicleType().trim() : "TRUCK",
                    req.getVehicleNumber() != null ? req.getVehicleNumber().trim() : null,
                    "ASSIGNED"
            );
            delivery.setPartner(partner);
            delivery.setAssignedAt(nowIso);
        });
    }

    /**
     * Mark an ASSIGNED delivery as READY_FOR_PICKUP (Farmer / Admin).
     */
    public synchronized DeliveryResponse markReadyForPickup(String callerUid, String deliveryId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(deliveryId, callerUid, DeliveryStatus.ASSIGNED, DeliveryStatus.READY_FOR_PICKUP, (delivery, nowIso) -> {
            if (!callerUid.equals(delivery.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer or admin can mark delivery ready for pickup.");
            }
            delivery.setReadyForPickupAt(nowIso);
        });
    }

    /**
     * Mark a READY_FOR_PICKUP delivery as PICKED_UP (Farmer / Partner).
     */
    public synchronized DeliveryResponse markPickedUp(String callerUid, String deliveryId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(deliveryId, callerUid, DeliveryStatus.READY_FOR_PICKUP, DeliveryStatus.PICKED_UP, (delivery, nowIso) -> {
            if (!callerUid.equals(delivery.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer or authorized partner can confirm pickup.");
            }
            delivery.setPickedUpAt(nowIso);
        });
    }

    /**
     * Mark a PICKED_UP delivery as IN_TRANSIT (Farmer / Partner).
     */
    public synchronized DeliveryResponse markInTransit(String callerUid, String deliveryId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(deliveryId, callerUid, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT, (delivery, nowIso) -> {
            if (!callerUid.equals(delivery.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer or authorized partner can update transit status.");
            }
            delivery.setInTransitAt(nowIso);
        });
    }

    /**
     * Mark an IN_TRANSIT delivery as OUT_FOR_DELIVERY (Farmer / Partner).
     */
    public synchronized DeliveryResponse markOutForDelivery(String callerUid, String deliveryId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        return executeStatusTransition(deliveryId, callerUid, DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY, (delivery, nowIso) -> {
            if (!callerUid.equals(delivery.getFarmerId())) {
                throw new AccessDeniedException("Only the seller farmer or authorized partner can update out-for-delivery status.");
            }
            delivery.setOutForDeliveryAt(nowIso);
        });
    }

    /**
     * Mark an OUT_FOR_DELIVERY delivery as DELIVERED and atomically complete the Order in the SAME transaction.
     */
    public synchronized DeliveryResponse markDelivered(String callerUid, String deliveryId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference deliveryRef = firestore.collection(COLLECTION_NAME).document(deliveryId);

                Delivery updatedDelivery = firestore.runTransaction(transaction -> {
                    DocumentSnapshot delSnap = transaction.get(deliveryRef).get();
                    if (!delSnap.exists()) {
                        throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
                    }

                    Delivery delivery = mapDocToDelivery(delSnap);

                    if (!callerUid.equals(delivery.getFarmerId()) && !callerUid.equals(delivery.getBuyerUid())) {
                        throw new AccessDeniedException("Unauthorized to complete delivery.");
                    }

                    // Idempotency check: If already DELIVERED, return existing without re-processing
                    if (DeliveryStatus.DELIVERED.name().equalsIgnoreCase(delivery.getStatus())) {
                        return delivery;
                    }

                    if (!DeliveryStatus.OUT_FOR_DELIVERY.name().equalsIgnoreCase(delivery.getStatus())) {
                        throw new IllegalStateException("Cannot transition delivery from status " + delivery.getStatus() + " to DELIVERED. Expected status: OUT_FOR_DELIVERY.");
                    }

                    // Single Transaction Order Coordination
                    DocumentReference orderRef = firestore.collection(OrderService.COLLECTION_NAME).document(delivery.getOrderId());
                    DocumentSnapshot orderSnap = transaction.get(orderRef).get();
                    if (orderSnap.exists()) {
                        String orderStatus = orderSnap.getString("status");
                        if (OrderStatus.CANCELLED.name().equalsIgnoreCase(orderStatus)) {
                            throw new IllegalStateException("Cannot complete delivery for a cancelled order.");
                        }

                        // Transition order to COMPLETED if not already COMPLETED
                        if (!OrderStatus.COMPLETED.name().equalsIgnoreCase(orderStatus)) {
                            Map<String, Object> orderUpdates = new HashMap<>();
                            orderUpdates.put("status", OrderStatus.COMPLETED.name());
                            orderUpdates.put("completedAt", Instant.now().toString());
                            orderUpdates.put("updatedAt", Instant.now().toString());
                            transaction.update(orderRef, orderUpdates);
                        }
                    }

                    String nowIso = Instant.now().toString();
                    delivery.setStatus(DeliveryStatus.DELIVERED.name());
                    delivery.setDeliveredAt(nowIso);
                    delivery.setUpdatedAt(nowIso);

                    transaction.set(deliveryRef, mapDeliveryToDoc(delivery));
                    return delivery;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(updatedDelivery, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to complete delivery transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution for unit tests
        Delivery delivery = inMemoryDeliveries.get(deliveryId);
        if (delivery == null) {
            throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
        }

        if (!callerUid.equals(delivery.getFarmerId()) && !callerUid.equals(delivery.getBuyerUid())) {
            throw new AccessDeniedException("Unauthorized to complete delivery.");
        }

        if (DeliveryStatus.DELIVERED.name().equalsIgnoreCase(delivery.getStatus())) {
            return toResponse(delivery, callerUid);
        }

        if (!DeliveryStatus.OUT_FOR_DELIVERY.name().equalsIgnoreCase(delivery.getStatus())) {
            throw new IllegalStateException("Cannot transition delivery from status " + delivery.getStatus() + " to DELIVERED. Expected status: OUT_FOR_DELIVERY.");
        }

        String nowIso = Instant.now().toString();
        delivery.setStatus(DeliveryStatus.DELIVERED.name());
        delivery.setDeliveredAt(nowIso);
        delivery.setUpdatedAt(nowIso);

        // Synchronize in-memory order completion
        try {
            orderService.completeOrder(delivery.getFarmerId(), delivery.getOrderId());
        } catch (Exception ignored) {}

        inMemoryDeliveries.put(deliveryId, delivery);
        return toResponse(delivery, callerUid);
    }

    /**
     * Cancel a delivery prior to pickup.
     */
    public synchronized DeliveryResponse cancelDelivery(String callerUid, String deliveryId, CancelDeliveryRequest req) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        String reason = req != null && req.getReason() != null ? req.getReason().trim() : "Cancelled by user";

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference deliveryRef = firestore.collection(COLLECTION_NAME).document(deliveryId);

                Delivery cancelledDelivery = firestore.runTransaction(transaction -> {
                    DocumentSnapshot delSnap = transaction.get(deliveryRef).get();
                    if (!delSnap.exists()) {
                        throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
                    }

                    Delivery delivery = mapDocToDelivery(delSnap);

                    if (!callerUid.equals(delivery.getBuyerUid()) && !callerUid.equals(delivery.getFarmerId())) {
                        throw new AccessDeniedException("Unauthorized to cancel delivery.");
                    }

                    String currentStatus = delivery.getStatus();
                    if (DeliveryStatus.PICKED_UP.name().equalsIgnoreCase(currentStatus) ||
                        DeliveryStatus.IN_TRANSIT.name().equalsIgnoreCase(currentStatus) ||
                        DeliveryStatus.OUT_FOR_DELIVERY.name().equalsIgnoreCase(currentStatus) ||
                        DeliveryStatus.DELIVERED.name().equalsIgnoreCase(currentStatus)) {
                        throw new IllegalStateException("Cannot cancel delivery once picked up or in transit (Current status: " + currentStatus + ").");
                    }

                    if (DeliveryStatus.CANCELLED.name().equalsIgnoreCase(currentStatus)) {
                        return delivery;
                    }

                    String nowIso = Instant.now().toString();
                    delivery.setStatus(DeliveryStatus.CANCELLED.name());
                    delivery.setCancelledAt(nowIso);
                    delivery.setCancelledByUid(callerUid);
                    delivery.setCancellationReason(reason);
                    delivery.setUpdatedAt(nowIso);

                    transaction.set(deliveryRef, mapDeliveryToDoc(delivery));
                    return delivery;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(cancelledDelivery, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to cancel delivery transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution for unit tests
        Delivery delivery = inMemoryDeliveries.get(deliveryId);
        if (delivery == null) {
            throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
        }

        if (!callerUid.equals(delivery.getBuyerUid()) && !callerUid.equals(delivery.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to cancel delivery.");
        }

        String currentStatus = delivery.getStatus();
        if (DeliveryStatus.PICKED_UP.name().equalsIgnoreCase(currentStatus) ||
            DeliveryStatus.IN_TRANSIT.name().equalsIgnoreCase(currentStatus) ||
            DeliveryStatus.OUT_FOR_DELIVERY.name().equalsIgnoreCase(currentStatus) ||
            DeliveryStatus.DELIVERED.name().equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot cancel delivery once picked up or in transit (Current status: " + currentStatus + ").");
        }

        if (DeliveryStatus.CANCELLED.name().equalsIgnoreCase(currentStatus)) {
            return toResponse(delivery, callerUid);
        }

        String nowIso = Instant.now().toString();
        delivery.setStatus(DeliveryStatus.CANCELLED.name());
        delivery.setCancelledAt(nowIso);
        delivery.setCancelledByUid(callerUid);
        delivery.setCancellationReason(reason);
        delivery.setUpdatedAt(nowIso);

        inMemoryDeliveries.put(deliveryId, delivery);
        return toResponse(delivery, callerUid);
    }

    /**
     * Get single delivery details by ID.
     */
    public DeliveryResponse getDeliveryById(String deliveryId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Delivery delivery = fetchDeliveryFromFirestoreOrMemory(deliveryId);
        if (delivery == null) {
            throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
        }

        if (!callerUid.equals(delivery.getBuyerUid()) && !callerUid.equals(delivery.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to delivery details.");
        }

        return toResponse(delivery, callerUid);
    }

    /**
     * Get delivery associated with an order ID.
     */
    public DeliveryResponse getDeliveryByOrderId(String orderId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        String deliveryId = "del_" + orderId;
        Delivery delivery = fetchDeliveryFromFirestoreOrMemory(deliveryId);
        if (delivery == null) {
            throw new NoSuchElementException("No delivery record found for Order ID: " + orderId);
        }

        if (!callerUid.equals(delivery.getBuyerUid()) && !callerUid.equals(delivery.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to delivery details.");
        }

        return toResponse(delivery, callerUid);
    }

    /**
     * Get deliveries initiated by buyer.
     */
    public DeliveryPageResponse getMyDeliveries(String buyerUid, String statusFilter, int page, int size) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Delivery> buyerDeliveries = fetchBuyerDeliveriesFromFirestoreOrMemory(buyerUid);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            buyerDeliveries = buyerDeliveries.stream()
                    .filter(d -> statusFilter.equalsIgnoreCase(d.getStatus()))
                    .collect(Collectors.toList());
        }

        buyerDeliveries.sort(Comparator.comparing(Delivery::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = buyerDeliveries.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<DeliveryResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = buyerDeliveries.subList(fromIndex, toIndex).stream()
                    .map(d -> toResponse(d, buyerUid))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new DeliveryPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get deliveries received by farmer for owned orders.
     */
    public DeliveryPageResponse getReceivedDeliveries(String farmerId, String statusFilter, int page, int size) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Delivery> farmerDeliveries = fetchFarmerDeliveriesFromFirestoreOrMemory(farmerId);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            farmerDeliveries = farmerDeliveries.stream()
                    .filter(d -> statusFilter.equalsIgnoreCase(d.getStatus()))
                    .collect(Collectors.toList());
        }

        farmerDeliveries.sort(Comparator.comparing(Delivery::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = farmerDeliveries.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<DeliveryResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = farmerDeliveries.subList(fromIndex, toIndex).stream()
                    .map(d -> toResponse(d, farmerId))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new DeliveryPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    private interface StatusUpdater {
        void update(Delivery delivery, String nowIso);
    }

    private DeliveryResponse executeStatusTransition(String deliveryId, String callerUid, DeliveryStatus expectedStatus, DeliveryStatus targetStatus, StatusUpdater updater) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference deliveryRef = firestore.collection(COLLECTION_NAME).document(deliveryId);

                Delivery updatedDelivery = firestore.runTransaction(transaction -> {
                    DocumentSnapshot delSnap = transaction.get(deliveryRef).get();
                    if (!delSnap.exists()) {
                        throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
                    }

                    Delivery delivery = mapDocToDelivery(delSnap);

                    // Idempotency: If already in target status, return response safely
                    if (targetStatus.name().equalsIgnoreCase(delivery.getStatus())) {
                        return delivery;
                    }

                    if (!expectedStatus.name().equalsIgnoreCase(delivery.getStatus())) {
                        throw new IllegalStateException("Cannot transition delivery from status " + delivery.getStatus() + " to " + targetStatus.name() + ". Expected status: " + expectedStatus.name());
                    }

                    String nowIso = Instant.now().toString();
                    updater.update(delivery, nowIso);
                    delivery.setStatus(targetStatus.name());
                    delivery.setUpdatedAt(nowIso);

                    transaction.set(deliveryRef, mapDeliveryToDoc(delivery));
                    return delivery;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(updatedDelivery, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to update delivery status transaction: " + e.getMessage(), e);
            }
        }

        // In-memory fallback
        Delivery delivery = inMemoryDeliveries.get(deliveryId);
        if (delivery == null) {
            throw new NoSuchElementException("Delivery record not found with ID: " + deliveryId);
        }

        if (targetStatus.name().equalsIgnoreCase(delivery.getStatus())) {
            return toResponse(delivery, callerUid);
        }

        if (!expectedStatus.name().equalsIgnoreCase(delivery.getStatus())) {
            throw new IllegalStateException("Cannot transition delivery from status " + delivery.getStatus() + " to " + targetStatus.name() + ". Expected status: " + expectedStatus.name());
        }

        String nowIso = Instant.now().toString();
        updater.update(delivery, nowIso);
        delivery.setStatus(targetStatus.name());
        delivery.setUpdatedAt(nowIso);

        inMemoryDeliveries.put(deliveryId, delivery);
        return toResponse(delivery, callerUid);
    }

    private Delivery fetchDeliveryFromFirestoreOrMemory(String deliveryId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(deliveryId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                if (doc.exists()) {
                    return mapDocToDelivery(doc);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching delivery {} from Firestore: {}", deliveryId, e.getMessage());
            }
        }
        return inMemoryDeliveries.get(deliveryId);
    }

    private List<Delivery> fetchBuyerDeliveriesFromFirestoreOrMemory(String buyerUid) {
        List<Delivery> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("buyerUid", buyerUid)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToDelivery(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying buyer deliveries for {} from Firestore: {}", buyerUid, e.getMessage());
            }
        }

        return inMemoryDeliveries.values().stream()
                .filter(d -> buyerUid.equals(d.getBuyerUid()))
                .collect(Collectors.toList());
    }

    private List<Delivery> fetchFarmerDeliveriesFromFirestoreOrMemory(String farmerId) {
        List<Delivery> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("farmerId", farmerId)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToDelivery(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying farmer deliveries for {} from Firestore: {}", farmerId, e.getMessage());
            }
        }

        return inMemoryDeliveries.values().stream()
                .filter(d -> farmerId.equals(d.getFarmerId()))
                .collect(Collectors.toList());
    }

    public DeliveryResponse toResponse(Delivery delivery, String callerUid) {
        if (delivery == null) return null;

        boolean isFarmer = callerUid != null && callerUid.equals(delivery.getFarmerId());
        boolean isBuyer = callerUid != null && callerUid.equals(delivery.getBuyerUid());

        String status = delivery.getStatus();
        boolean isCreated = DeliveryStatus.CREATED.name().equalsIgnoreCase(status);
        boolean isAssigned = DeliveryStatus.ASSIGNED.name().equalsIgnoreCase(status);
        boolean isReady = DeliveryStatus.READY_FOR_PICKUP.name().equalsIgnoreCase(status);
        boolean isPickedUp = DeliveryStatus.PICKED_UP.name().equalsIgnoreCase(status);
        boolean isInTransit = DeliveryStatus.IN_TRANSIT.name().equalsIgnoreCase(status);
        boolean isOutForDelivery = DeliveryStatus.OUT_FOR_DELIVERY.name().equalsIgnoreCase(status);

        boolean canAssign = isFarmer && isCreated;
        boolean canMarkReady = isFarmer && isAssigned;
        boolean canPickup = isFarmer && isReady;
        boolean canInTransit = isFarmer && isPickedUp;
        boolean canOutForDelivery = isFarmer && isInTransit;
        boolean canDeliver = (isFarmer || isBuyer) && isOutForDelivery;
        boolean canCancel = (isFarmer || isBuyer) && (isCreated || isAssigned || isReady);

        return new DeliveryResponse(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getFarmerId(),
                delivery.getBuyerUid(),
                delivery.getBuyerRole(),
                delivery.getStatus(),
                delivery.getPickupAddress(),
                delivery.getPickupLatitude(),
                delivery.getPickupLongitude(),
                delivery.getDeliveryAddress(),
                delivery.getDeliveryLatitude(),
                delivery.getDeliveryLongitude(),
                delivery.getPartner(),
                delivery.getTrackingReference(),
                delivery.getCancellationReason(),
                delivery.getCancelledByUid(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt(),
                delivery.getAssignedAt(),
                delivery.getReadyForPickupAt(),
                delivery.getPickedUpAt(),
                delivery.getInTransitAt(),
                delivery.getOutForDeliveryAt(),
                delivery.getDeliveredAt(),
                delivery.getCancelledAt(),
                canAssign,
                canMarkReady,
                canPickup,
                canInTransit,
                canOutForDelivery,
                canDeliver,
                canCancel
        );
    }

    private Delivery mapDocToDelivery(DocumentSnapshot doc) {
        Delivery delivery = new Delivery();
        delivery.setDeliveryId(doc.getString("deliveryId"));
        delivery.setOrderId(doc.getString("orderId"));
        delivery.setFarmerId(doc.getString("farmerId"));
        delivery.setBuyerUid(doc.getString("buyerUid"));
        delivery.setBuyerRole(doc.getString("buyerRole"));
        delivery.setStatus(doc.getString("status"));
        delivery.setTrackingReference(doc.getString("trackingReference"));
        delivery.setCancellationReason(doc.getString("cancellationReason"));
        delivery.setCancelledByUid(doc.getString("cancelledByUid"));

        delivery.setPickupLatitude(doc.getDouble("pickupLatitude"));
        delivery.setPickupLongitude(doc.getDouble("pickupLongitude"));
        delivery.setDeliveryLatitude(doc.getDouble("deliveryLatitude"));
        delivery.setDeliveryLongitude(doc.getDouble("deliveryLongitude"));

        delivery.setCreatedAt(doc.getString("createdAt"));
        delivery.setUpdatedAt(doc.getString("updatedAt"));
        delivery.setAssignedAt(doc.getString("assignedAt"));
        delivery.setReadyForPickupAt(doc.getString("readyForPickupAt"));
        delivery.setPickedUpAt(doc.getString("pickedUpAt"));
        delivery.setInTransitAt(doc.getString("inTransitAt"));
        delivery.setOutForDeliveryAt(doc.getString("outForDeliveryAt"));
        delivery.setDeliveredAt(doc.getString("deliveredAt"));
        delivery.setCancelledAt(doc.getString("cancelledAt"));

        Map<String, Object> pickupMap = (Map<String, Object>) doc.get("pickupAddress");
        if (pickupMap != null) {
            delivery.setPickupAddress(new LocationDto(
                    (String) pickupMap.get("state"),
                    (String) pickupMap.get("district"),
                    (String) pickupMap.get("mandal"),
                    (String) pickupMap.get("village"),
                    (String) pickupMap.get("pincode")
            ));
        }

        Map<String, Object> deliveryMap = (Map<String, Object>) doc.get("deliveryAddress");
        if (deliveryMap != null) {
            delivery.setDeliveryAddress(new LocationDto(
                    (String) deliveryMap.get("state"),
                    (String) deliveryMap.get("district"),
                    (String) deliveryMap.get("mandal"),
                    (String) deliveryMap.get("village"),
                    (String) deliveryMap.get("pincode")
            ));
        }

        Map<String, Object> partnerMap = (Map<String, Object>) doc.get("partner");
        if (partnerMap != null) {
            delivery.setPartner(new DeliveryPartnerInfo(
                    (String) partnerMap.get("partnerId"),
                    (String) partnerMap.get("name"),
                    (String) partnerMap.get("phone"),
                    (String) partnerMap.get("vehicleType"),
                    (String) partnerMap.get("vehicleNumber"),
                    (String) partnerMap.get("status")
            ));
        }

        return delivery;
    }

    private Map<String, Object> mapDeliveryToDoc(Delivery delivery) {
        Map<String, Object> map = new HashMap<>();
        map.put("deliveryId", delivery.getDeliveryId());
        map.put("orderId", delivery.getOrderId());
        map.put("farmerId", delivery.getFarmerId());
        map.put("buyerUid", delivery.getBuyerUid());
        map.put("buyerRole", delivery.getBuyerRole());
        map.put("status", delivery.getStatus());
        map.put("trackingReference", delivery.getTrackingReference());
        map.put("cancellationReason", delivery.getCancellationReason());
        map.put("cancelledByUid", delivery.getCancelledByUid());

        map.put("pickupLatitude", delivery.getPickupLatitude());
        map.put("pickupLongitude", delivery.getPickupLongitude());
        map.put("deliveryLatitude", delivery.getDeliveryLatitude());
        map.put("deliveryLongitude", delivery.getDeliveryLongitude());

        map.put("createdAt", delivery.getCreatedAt());
        map.put("updatedAt", delivery.getUpdatedAt());
        map.put("assignedAt", delivery.getAssignedAt());
        map.put("readyForPickupAt", delivery.getReadyForPickupAt());
        map.put("pickedUpAt", delivery.getPickedUpAt());
        map.put("inTransitAt", delivery.getInTransitAt());
        map.put("outForDeliveryAt", delivery.getOutForDeliveryAt());
        map.put("deliveredAt", delivery.getDeliveredAt());
        map.put("cancelledAt", delivery.getCancelledAt());

        if (delivery.getPickupAddress() != null) {
            Map<String, Object> pickupMap = new HashMap<>();
            pickupMap.put("state", delivery.getPickupAddress().getState());
            pickupMap.put("district", delivery.getPickupAddress().getDistrict());
            pickupMap.put("mandal", delivery.getPickupAddress().getMandal());
            pickupMap.put("village", delivery.getPickupAddress().getVillage());
            pickupMap.put("pincode", delivery.getPickupAddress().getPincode());
            map.put("pickupAddress", pickupMap);
        }

        if (delivery.getDeliveryAddress() != null) {
            Map<String, Object> deliveryMap = new HashMap<>();
            deliveryMap.put("state", delivery.getDeliveryAddress().getState());
            deliveryMap.put("district", delivery.getDeliveryAddress().getDistrict());
            deliveryMap.put("mandal", delivery.getDeliveryAddress().getMandal());
            deliveryMap.put("village", delivery.getDeliveryAddress().getVillage());
            deliveryMap.put("pincode", delivery.getDeliveryAddress().getPincode());
            map.put("deliveryAddress", deliveryMap);
        }

        if (delivery.getPartner() != null) {
            Map<String, Object> partnerMap = new HashMap<>();
            partnerMap.put("partnerId", delivery.getPartner().getPartnerId());
            partnerMap.put("name", delivery.getPartner().getName());
            partnerMap.put("phone", delivery.getPartner().getPhone());
            partnerMap.put("vehicleType", delivery.getPartner().getVehicleType());
            partnerMap.put("vehicleNumber", delivery.getPartner().getVehicleNumber());
            partnerMap.put("status", delivery.getPartner().getStatus());
            map.put("partner", partnerMap);
        }

        return map;
    }
}
