package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.Offer;
import com.farmlink.api.model.OfferRound;
import com.farmlink.api.model.OfferStatus;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);

    public static final String COLLECTION_NAME = "offers";
    public static final int MAX_NEGOTIATION_ROUNDS = 10;
    public static final int DEFAULT_EXPIRATION_DAYS = 7;

    private final Firestore firestore;
    private final ProductListingService listingService;
    private final ProfileService profileService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback for offline/unit test execution
    private final Map<String, Offer> inMemoryOffers = new ConcurrentHashMap<>();

    @Autowired
    public OfferService(
            @Autowired(required = false) Firestore firestore,
            ProductListingService listingService,
            ProfileService profileService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.listingService = listingService;
        this.profileService = profileService;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Resolves the primary role of a user.
     */
    public String resolveUserRole(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (userDoc.exists() && userDoc.getString("role") != null) {
                    return userDoc.getString("role");
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not fetch role from Firestore for uid {}: {}", uid, e.getMessage());
            }
        }
        // ProfileService fallback
        try {
            ProfileResponse p = profileService.getProfile(uid, null, false, null);
            if (p != null && p.getRole() != null) {
                return p.getRole();
            }
        } catch (Exception ignored) {}

        return ProfileService.ROLE_USER;
    }

    /**
     * Creates a new offer against an active marketplace listing.
     * Buyer identity and role are resolved exclusively from server-side authenticated token.
     */
    public synchronized OfferResponse createOffer(String buyerUid, CreateOfferRequest request) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Create offer request cannot be null.");
        }

        String buyerRole = resolveUserRole(buyerUid);
        if (ProfileService.ROLE_FARMER.equalsIgnoreCase(buyerRole)) {
            // Note: If user is a Farmer, verify if they are attempting to offer on listing
            // Standard farmer role cannot initiate offers on listings unless specifically buying in customer/mediator capacity
            // Per requirement: FARMER cannot make offer against their own listing. Also buyers are MEDIATOR_BUYER / CUSTOMER.
        }

        // Fetch target listing
        ProductListingResponse listing = listingService.getListingById(request.getListingId(), buyerUid);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + request.getListingId());
        }

        // Validate listing status
        if (!"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Offers can only be submitted against ACTIVE marketplace listings. Current status: " + listing.getStatus());
        }

        // Validate listing ownership (farmer cannot offer on own listing)
        if (buyerUid.equals(listing.getOwnerUid())) {
            throw new IllegalArgumentException("Listing owners cannot make offers against their own listings.");
        }

        // Validate Offered Quantity
        if (request.getOfferedQuantity() == null || request.getOfferedQuantity() <= 0) {
            throw new IllegalArgumentException("Offered quantity must be greater than zero.");
        }
        if (listing.getAvailableQuantity() != null && request.getOfferedQuantity() > listing.getAvailableQuantity()) {
            throw new IllegalArgumentException("Offered quantity (" + request.getOfferedQuantity() + ") cannot exceed available quantity (" + listing.getAvailableQuantity() + ").");
        }

        // Validate Unit
        String unit = (request.getQuantityUnit() != null) ? request.getQuantityUnit().trim().toUpperCase(Locale.ROOT) : listing.getUnit();
        if (!ProductListingService.SUPPORTED_UNITS.contains(unit)) {
            throw new IllegalArgumentException("Unsupported unit specified: " + request.getQuantityUnit());
        }

        // Validate Offered Price
        if (request.getOfferedPrice() == null || request.getOfferedPrice() <= 0) {
            throw new IllegalArgumentException("Offered price must be greater than zero.");
        }
        String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : listing.getPriceUnit();

        String offerId = "offer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nowIso = Instant.now().toString();
        String expiresIso = Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS).toString();

        OfferRound initialRound = new OfferRound(
                1,
                buyerUid,
                buyerRole,
                "OFFER",
                request.getOfferedQuantity(),
                unit,
                request.getOfferedPrice(),
                priceUnit,
                request.getMessage(),
                nowIso
        );

        Offer offer = new Offer(
                offerId,
                listing.getListingId(),
                listing.getOwnerUid(), // farmerId
                buyerUid,
                buyerRole,
                listing.getCropId(),
                listing.getCropName(),
                request.getOfferedQuantity(),
                unit,
                request.getOfferedPrice(),
                priceUnit,
                listing.getAskingPrice(), // snapshot of farmer asking price reference
                request.getMessage(),
                OfferStatus.PENDING.name(),
                1,
                listing.getOwnerUid(), // currentResponderUid is the farmer
                new ArrayList<>(List.of(initialRound)),
                nowIso,
                nowIso,
                expiresIso
        );

        saveOfferToFirestore(offer);
        inMemoryOffers.put(offerId, offer);

        return toResponse(offer, buyerUid);
    }

    /**
     * Get offer detail by offerId with participant authorization.
     */
    public OfferResponse getOfferById(String offerId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        // Authorization check: Caller must be offer buyer OR listing farmer
        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        // Lazy expiration check
        checkAndApplyLazyExpiration(offer);

        return toResponse(offer, callerUid);
    }

    /**
     * Get offers initiated by authenticated buyer/customer.
     */
    public OfferPageResponse getMyOffers(String buyerUid, String statusFilter, int page, int size) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Offer> buyerOffers = fetchBuyerOffersFromFirestoreOrMemory(buyerUid);

        // Apply lazy expiration to candidate offers
        for (Offer o : buyerOffers) {
            checkAndApplyLazyExpiration(o);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            buyerOffers = buyerOffers.stream()
                    .filter(o -> statusFilter.equalsIgnoreCase(o.getStatus()))
                    .collect(Collectors.toList());
        }

        buyerOffers.sort(Comparator.comparing(Offer::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = buyerOffers.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<OfferResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = buyerOffers.subList(fromIndex, toIndex).stream()
                    .map(o -> toResponse(o, buyerUid))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new OfferPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get offers received by authenticated farmer for listings they own.
     */
    public OfferPageResponse getReceivedOffers(String farmerId, String statusFilter, int page, int size) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Offer> farmerOffers = fetchFarmerOffersFromFirestoreOrMemory(farmerId);

        // Apply lazy expiration
        for (Offer o : farmerOffers) {
            checkAndApplyLazyExpiration(o);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            farmerOffers = farmerOffers.stream()
                    .filter(o -> statusFilter.equalsIgnoreCase(o.getStatus()))
                    .collect(Collectors.toList());
        }

        farmerOffers.sort(Comparator.comparing(Offer::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = farmerOffers.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<OfferResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = farmerOffers.subList(fromIndex, toIndex).stream()
                    .map(o -> toResponse(o, farmerId))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new OfferPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Counter an active offer/proposal.
     */
    public synchronized OfferResponse counterOffer(String callerUid, String offerId, CounterOfferRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Counter offer request cannot be null.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        // Participant check
        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        // Responder check (must be caller's turn)
        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to respond to this negotiation round.");
        }

        // Terminal state check
        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot counter an offer in terminal state: " + offer.getStatus());
        }

        // Check Round Limit
        if (offer.getRoundNumber() >= MAX_NEGOTIATION_ROUNDS) {
            throw new IllegalArgumentException("Maximum negotiation rounds reached (" + MAX_NEGOTIATION_ROUNDS + "). Further counter-offers are not allowed.");
        }

        // Validate listing availability
        ProductListingResponse listing = listingService.getListingById(offer.getListingId(), callerUid);
        if (listing == null || !"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Cannot counter offer because the marketplace listing is no longer active.");
        }

        // Validate Counter Quantity
        if (request.getCounterQuantity() == null || request.getCounterQuantity() <= 0) {
            throw new IllegalArgumentException("Counter quantity must be greater than zero.");
        }
        if (listing.getAvailableQuantity() != null && request.getCounterQuantity() > listing.getAvailableQuantity()) {
            throw new IllegalArgumentException("Counter quantity (" + request.getCounterQuantity() + ") cannot exceed available quantity (" + listing.getAvailableQuantity() + ").");
        }

        // Validate Counter Price
        if (request.getCounterPrice() == null || request.getCounterPrice() <= 0) {
            throw new IllegalArgumentException("Counter price must be greater than zero.");
        }

        String unit = (request.getQuantityUnit() != null) ? request.getQuantityUnit().trim().toUpperCase(Locale.ROOT) : offer.getQuantityUnit();
        String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : offer.getPriceUnit();

        int nextRoundNumber = offer.getRoundNumber() + 1;
        String callerRole = resolveUserRole(callerUid);
        String nextResponderUid = callerUid.equals(offer.getFarmerId()) ? offer.getBuyerUid() : offer.getFarmerId();
        String nowIso = Instant.now().toString();

        OfferRound round = new OfferRound(
                nextRoundNumber,
                callerUid,
                callerRole,
                "COUNTER",
                request.getCounterQuantity(),
                unit,
                request.getCounterPrice(),
                priceUnit,
                request.getMessage(),
                nowIso
        );

        offer.setOfferedQuantity(request.getCounterQuantity());
        offer.setQuantityUnit(unit);
        offer.setOfferedPrice(request.getCounterPrice());
        offer.setPriceUnit(priceUnit);
        offer.setMessage(request.getMessage());
        offer.setStatus(OfferStatus.COUNTERED.name());
        offer.setRoundNumber(nextRoundNumber);
        offer.setCurrentResponderUid(nextResponderUid);
        offer.getRounds().add(round);
        offer.setUpdatedAt(nowIso);
        offer.setRespondedAt(nowIso);

        saveOfferToFirestore(offer);
        inMemoryOffers.put(offerId, offer);

        return toResponse(offer, callerUid);
    }

    /**
     * Accept an active offer/proposal.
     */
    public synchronized OfferResponse acceptOffer(String callerUid, String offerId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        // Participant check
        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        // Responder check (must be caller's turn to respond/accept)
        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to accept this proposal.");
        }

        // Terminal state check
        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot accept an offer in terminal state: " + offer.getStatus());
        }

        // Revalidate listing status and available quantity at acceptance time
        ProductListingResponse listing = listingService.getListingById(offer.getListingId(), callerUid);
        if (listing == null || !"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Cannot accept offer because marketplace listing is no longer ACTIVE.");
        }
        if (listing.getAvailableQuantity() != null && offer.getOfferedQuantity() > listing.getAvailableQuantity()) {
            throw new IllegalArgumentException("Cannot accept offer. Offered quantity (" + offer.getOfferedQuantity() + ") exceeds available quantity (" + listing.getAvailableQuantity() + ").");
        }

        int nextRoundNumber = offer.getRoundNumber() + 1;
        String callerRole = resolveUserRole(callerUid);
        String nowIso = Instant.now().toString();

        OfferRound acceptRound = new OfferRound(
                nextRoundNumber,
                callerUid,
                callerRole,
                "ACCEPT",
                offer.getOfferedQuantity(),
                offer.getQuantityUnit(),
                offer.getOfferedPrice(),
                offer.getPriceUnit(),
                "Proposal Accepted",
                nowIso
        );

        offer.setStatus(OfferStatus.ACCEPTED.name());
        offer.setRoundNumber(nextRoundNumber);
        offer.setAgreedQuantity(offer.getOfferedQuantity());
        offer.setAgreedPrice(offer.getOfferedPrice());
        offer.setAgreedPriceUnit(offer.getPriceUnit());
        offer.getRounds().add(acceptRound);
        offer.setUpdatedAt(nowIso);
        offer.setRespondedAt(nowIso);

        saveOfferToFirestore(offer);
        inMemoryOffers.put(offerId, offer);

        return toResponse(offer, callerUid);
    }

    /**
     * Reject an active offer/proposal.
     */
    public synchronized OfferResponse rejectOffer(String callerUid, String offerId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        // Participant check
        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        // Responder check
        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to reject this proposal.");
        }

        // Terminal state check
        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot reject an offer in terminal state: " + offer.getStatus());
        }

        int nextRoundNumber = offer.getRoundNumber() + 1;
        String callerRole = resolveUserRole(callerUid);
        String nowIso = Instant.now().toString();

        OfferRound rejectRound = new OfferRound(
                nextRoundNumber,
                callerUid,
                callerRole,
                "REJECT",
                offer.getOfferedQuantity(),
                offer.getQuantityUnit(),
                offer.getOfferedPrice(),
                offer.getPriceUnit(),
                "Proposal Rejected",
                nowIso
        );

        offer.setStatus(OfferStatus.REJECTED.name());
        offer.setRoundNumber(nextRoundNumber);
        offer.getRounds().add(rejectRound);
        offer.setUpdatedAt(nowIso);
        offer.setRespondedAt(nowIso);

        saveOfferToFirestore(offer);
        inMemoryOffers.put(offerId, offer);

        return toResponse(offer, callerUid);
    }

    /**
     * Cancel a pending offer (initiator buyer only).
     */
    public synchronized OfferResponse cancelOffer(String buyerUid, String offerId) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        // Only offer creator (buyerUid) can cancel
        if (!buyerUid.equals(offer.getBuyerUid())) {
            throw new AccessDeniedException("Only the offer initiator can cancel this offer.");
        }

        // Terminal state check
        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot cancel an offer in terminal state: " + offer.getStatus());
        }

        int nextRoundNumber = offer.getRoundNumber() + 1;
        String callerRole = resolveUserRole(buyerUid);
        String nowIso = Instant.now().toString();

        OfferRound cancelRound = new OfferRound(
                nextRoundNumber,
                buyerUid,
                callerRole,
                "CANCEL",
                offer.getOfferedQuantity(),
                offer.getQuantityUnit(),
                offer.getOfferedPrice(),
                offer.getPriceUnit(),
                "Offer Cancelled by Buyer",
                nowIso
        );

        offer.setStatus(OfferStatus.CANCELLED.name());
        offer.setRoundNumber(nextRoundNumber);
        offer.getRounds().add(cancelRound);
        offer.setUpdatedAt(nowIso);
        offer.setRespondedAt(nowIso);

        saveOfferToFirestore(offer);
        inMemoryOffers.put(offerId, offer);

        return toResponse(offer, buyerUid);
    }

    /**
     * Retrieve complete negotiation timeline history for an offer.
     */
    public List<OfferRoundResponse> getOfferHistory(String offerId, String callerUid) {
        OfferResponse response = getOfferById(offerId, callerUid);
        return response.getRounds() != null ? response.getRounds() : Collections.emptyList();
    }

    // ==========================================
    // HELPERS & PERSISTENCE
    // ==========================================

    private boolean isTerminalStatus(String status) {
        return OfferStatus.ACCEPTED.name().equalsIgnoreCase(status)
                || OfferStatus.REJECTED.name().equalsIgnoreCase(status)
                || OfferStatus.CANCELLED.name().equalsIgnoreCase(status)
                || OfferStatus.EXPIRED.name().equalsIgnoreCase(status);
    }

    private void checkAndApplyLazyExpiration(Offer offer) {
        if (offer == null || isTerminalStatus(offer.getStatus())) return;

        if (offer.getExpiresAt() != null) {
            try {
                Instant expiry = Instant.parse(offer.getExpiresAt());
                if (Instant.now().isAfter(expiry)) {
                    offer.setStatus(OfferStatus.EXPIRED.name());
                    offer.setUpdatedAt(Instant.now().toString());
                    saveOfferToFirestore(offer);
                    inMemoryOffers.put(offer.getOfferId(), offer);
                }
            } catch (Exception ignored) {}
        }
    }

    private OfferResponse toResponse(Offer offer, String callerUid) {
        if (offer == null) return null;

        boolean isParticipant = callerUid != null && (callerUid.equals(offer.getBuyerUid()) || callerUid.equals(offer.getFarmerId()));
        boolean isResponder = callerUid != null && callerUid.equals(offer.getCurrentResponderUid());
        boolean isInitiator = callerUid != null && callerUid.equals(offer.getBuyerUid());
        boolean activeState = !isTerminalStatus(offer.getStatus());

        boolean canAccept = isParticipant && isResponder && activeState;
        boolean canReject = isParticipant && isResponder && activeState;
        boolean canCounter = isParticipant && isResponder && activeState && (offer.getRoundNumber() < MAX_NEGOTIATION_ROUNDS);
        boolean canCancel = isInitiator && activeState;

        List<OfferRoundResponse> roundResponses = offer.getRounds() != null ?
                offer.getRounds().stream()
                        .map(r -> new OfferRoundResponse(
                                r.getRoundNumber(),
                                r.getSenderUid(),
                                r.getSenderRole(),
                                r.getAction(),
                                r.getQuantity(),
                                r.getQuantityUnit(),
                                r.getPrice(),
                                r.getPriceUnit(),
                                r.getMessage(),
                                r.getTimestamp()
                        )).collect(Collectors.toList())
                : Collections.emptyList();

        return new OfferResponse(
                offer.getOfferId(),
                offer.getListingId(),
                offer.getFarmerId(),
                offer.getBuyerUid(),
                offer.getBuyerRole(),
                offer.getCropId(),
                offer.getCropName(),
                offer.getOfferedQuantity(),
                offer.getQuantityUnit(),
                offer.getOfferedPrice(),
                offer.getPriceUnit(),
                offer.getAskingPriceReference(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getRoundNumber(),
                offer.getCurrentResponderUid(),
                offer.getAgreedQuantity(),
                offer.getAgreedPrice(),
                offer.getAgreedPriceUnit(),
                roundResponses,
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                offer.getExpiresAt(),
                offer.getRespondedAt(),
                canAccept,
                canReject,
                canCounter,
                canCancel
        );
    }

    private void saveOfferToFirestore(Offer offer) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Map<String, Object> doc = new HashMap<>();
                doc.put("offerId", offer.getOfferId());
                doc.put("listingId", offer.getListingId());
                doc.put("farmerId", offer.getFarmerId());
                doc.put("buyerUid", offer.getBuyerUid());
                doc.put("buyerRole", offer.getBuyerRole());
                doc.put("cropId", offer.getCropId());
                doc.put("cropName", offer.getCropName());
                doc.put("offeredQuantity", offer.getOfferedQuantity());
                doc.put("quantityUnit", offer.getQuantityUnit());
                doc.put("offeredPrice", offer.getOfferedPrice());
                doc.put("priceUnit", offer.getPriceUnit());
                doc.put("askingPriceReference", offer.getAskingPriceReference());
                doc.put("message", offer.getMessage());
                doc.put("status", offer.getStatus());
                doc.put("roundNumber", offer.getRoundNumber());
                doc.put("currentResponderUid", offer.getCurrentResponderUid());
                doc.put("agreedQuantity", offer.getAgreedQuantity());
                doc.put("agreedPrice", offer.getAgreedPrice());
                doc.put("agreedPriceUnit", offer.getAgreedPriceUnit());

                // Embed rounds as structured maps in single document
                List<Map<String, Object>> roundsList = new ArrayList<>();
                if (offer.getRounds() != null) {
                    for (OfferRound r : offer.getRounds()) {
                        Map<String, Object> rMap = new HashMap<>();
                        rMap.put("roundNumber", r.getRoundNumber());
                        rMap.put("senderUid", r.getSenderUid());
                        rMap.put("senderRole", r.getSenderRole());
                        rMap.put("action", r.getAction());
                        rMap.put("quantity", r.getQuantity());
                        rMap.put("quantityUnit", r.getQuantityUnit());
                        rMap.put("price", r.getPrice());
                        rMap.put("priceUnit", r.getPriceUnit());
                        rMap.put("message", r.getMessage());
                        rMap.put("timestamp", r.getTimestamp());
                        roundsList.add(rMap);
                    }
                }
                doc.put("rounds", roundsList);
                doc.put("createdAt", offer.getCreatedAt());
                doc.put("updatedAt", offer.getUpdatedAt());
                doc.put("expiresAt", offer.getExpiresAt());
                doc.put("respondedAt", offer.getRespondedAt());

                firestore.collection(COLLECTION_NAME).document(offer.getOfferId()).set(doc, SetOptions.merge()).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not save offer {} to Firestore: {}", offer.getOfferId(), e.getMessage());
            }
        }
    }

    private Offer fetchOfferFromFirestoreOrMemory(String offerId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot snap = firestore.collection(COLLECTION_NAME).document(offerId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (snap.exists()) {
                    return mapDocToOffer(snap);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching offer {} from Firestore: {}", offerId, e.getMessage());
            }
        }
        return inMemoryOffers.get(offerId);
    }

    private List<Offer> fetchBuyerOffersFromFirestoreOrMemory(String buyerUid) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("buyerUid", buyerUid);

                QuerySnapshot querySnap = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<Offer> list = new ArrayList<>();
                for (DocumentSnapshot doc : querySnap.getDocuments()) {
                    list.add(mapDocToOffer(doc));
                }
                return list;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying buyer offers from Firestore for buyer {}: {}", buyerUid, e.getMessage());
            }
        }

        return inMemoryOffers.values().stream()
                .filter(o -> buyerUid.equals(o.getBuyerUid()))
                .collect(Collectors.toList());
    }

    private List<Offer> fetchFarmerOffersFromFirestoreOrMemory(String farmerId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("farmerId", farmerId);

                QuerySnapshot querySnap = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<Offer> list = new ArrayList<>();
                for (DocumentSnapshot doc : querySnap.getDocuments()) {
                    list.add(mapDocToOffer(doc));
                }
                return list;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying farmer offers from Firestore for farmer {}: {}", farmerId, e.getMessage());
            }
        }

        return inMemoryOffers.values().stream()
                .filter(o -> farmerId.equals(o.getFarmerId()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Offer mapDocToOffer(DocumentSnapshot doc) {
        List<OfferRound> rounds = new ArrayList<>();
        List<Map<String, Object>> roundsList = (List<Map<String, Object>>) doc.get("rounds");
        if (roundsList != null) {
            for (Map<String, Object> rMap : roundsList) {
                Number rNum = (Number) rMap.get("roundNumber");
                Number qty = (Number) rMap.get("quantity");
                Number prc = (Number) rMap.get("price");
                rounds.add(new OfferRound(
                        rNum != null ? rNum.intValue() : 1,
                        (String) rMap.get("senderUid"),
                        (String) rMap.get("senderRole"),
                        (String) rMap.get("action"),
                        qty != null ? qty.doubleValue() : null,
                        (String) rMap.get("quantityUnit"),
                        prc != null ? prc.doubleValue() : null,
                        (String) rMap.get("priceUnit"),
                        (String) rMap.get("message"),
                        (String) rMap.get("timestamp")
                ));
            }
        }

        Offer offer = new Offer(
                doc.getId(),
                doc.getString("listingId"),
                doc.getString("farmerId"),
                doc.getString("buyerUid"),
                doc.getString("buyerRole"),
                doc.getString("cropId"),
                doc.getString("cropName"),
                doc.getDouble("offeredQuantity"),
                doc.getString("quantityUnit"),
                doc.getDouble("offeredPrice"),
                doc.getString("priceUnit"),
                doc.getDouble("askingPriceReference"),
                doc.getString("message"),
                doc.getString("status"),
                doc.getLong("roundNumber") != null ? doc.getLong("roundNumber").intValue() : 1,
                doc.getString("currentResponderUid"),
                rounds,
                doc.getString("createdAt"),
                doc.getString("updatedAt"),
                doc.getString("expiresAt")
        );
        offer.setAgreedQuantity(doc.getDouble("agreedQuantity"));
        offer.setAgreedPrice(doc.getDouble("agreedPrice"));
        offer.setAgreedPriceUnit(doc.getString("agreedPriceUnit"));
        offer.setRespondedAt(doc.getString("respondedAt"));

        return offer;
    }
}
