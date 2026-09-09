package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.Offer;
import com.farmlink.api.model.OfferRound;
import com.farmlink.api.model.OfferStatus;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);

    public static final String COLLECTION_NAME = "offers";
    public static final int MAX_MESSAGE_LENGTH = 500;

    @Value("${marketplace.offers.max-rounds:10}")
    private int maxNegotiationRounds = 10;

    @Value("${marketplace.offers.default-expiration-days:7}")
    private int defaultExpirationDays = 7;

    private final Firestore firestore;
    private final ProductListingService listingService;
    private final ProfileService profileService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback ONLY for isolated offline unit tests (when firestore is null)
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

    public int getMaxNegotiationRounds() {
        return maxNegotiationRounds;
    }

    public void setMaxNegotiationRounds(int maxNegotiationRounds) {
        this.maxNegotiationRounds = maxNegotiationRounds;
    }

    public int getDefaultExpirationDays() {
        return defaultExpirationDays;
    }

    public void setDefaultExpirationDays(int defaultExpirationDays) {
        this.defaultExpirationDays = defaultExpirationDays;
    }

    /**
     * Resolves user primary role from Firestore users collection or ProfileService fallback.
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
        try {
            ProfileResponse p = profileService.getProfile(uid, null, false, null);
            if (p != null && p.getRole() != null) {
                return p.getRole();
            }
        } catch (Exception ignored) {}

        return ProfileService.ROLE_USER;
    }

    /**
     * Create a new commercial offer against an active marketplace listing.
     */
    public synchronized OfferResponse createOffer(String buyerUid, CreateOfferRequest request) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Create offer request cannot be null.");
        }

        String buyerRole = resolveUserRole(buyerUid);

        // Fetch target listing
        ProductListingResponse listing = listingService.getListingById(request.getListingId(), buyerUid);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + request.getListingId());
        }

        if (!"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Offers can only be submitted against ACTIVE marketplace listings. Current status: " + listing.getStatus());
        }

        if (buyerUid.equals(listing.getOwnerUid())) {
            throw new IllegalArgumentException("Listing owners cannot make offers against their own listings.");
        }

        // Validate Quantity
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

        // Validate Price
        if (request.getOfferedPrice() == null || request.getOfferedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Offered price must be greater than zero.");
        }
        String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : listing.getPriceUnit();

        String sanitizeMsg = sanitizeMessage(request.getMessage());

        String offerId = "offer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nowIso = Instant.now().toString();
        String expiresIso = Instant.now().plus(defaultExpirationDays, ChronoUnit.DAYS).toString();

        BigDecimal askingPriceRef = listing.getAskingPrice() != null ? BigDecimal.valueOf(listing.getAskingPrice()) : BigDecimal.ZERO;

        OfferRound initialRound = new OfferRound(
                1,
                buyerUid,
                buyerRole,
                "OFFER",
                request.getOfferedQuantity(),
                unit,
                request.getOfferedPrice(),
                priceUnit,
                sanitizeMsg,
                nowIso
        );

        Offer offer = new Offer(
                offerId,
                listing.getListingId(),
                listing.getOwnerUid(),
                buyerUid,
                buyerRole,
                listing.getCropId(),
                listing.getCropName(),
                request.getOfferedQuantity(),
                unit,
                request.getOfferedPrice(),
                priceUnit,
                askingPriceRef,
                sanitizeMsg,
                OfferStatus.PENDING.name(),
                1,
                listing.getOwnerUid(), // currentResponderUid is farmer
                new ArrayList<>(List.of(initialRound)),
                nowIso,
                nowIso,
                expiresIso
        );

        saveOfferToFirestore(offer);
        if (firestore == null) {
            inMemoryOffers.put(offerId, offer);
        }

        return toResponse(offer, buyerUid);
    }

    /**
     * Get single offer detail by ID.
     */
    public OfferResponse getOfferById(String offerId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Offer offer = fetchOfferFromFirestoreOrMemory(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        checkAndApplyLazyExpiration(offer);

        return toResponse(offer, callerUid);
    }

    /**
     * Get offers initiated by buyer.
     */
    public OfferPageResponse getMyOffers(String buyerUid, String statusFilter, int page, int size) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Offer> buyerOffers = fetchBuyerOffersFromFirestoreOrMemory(buyerUid);

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
     * Get offers received by farmer for owned listings.
     */
    public OfferPageResponse getReceivedOffers(String farmerId, String statusFilter, int page, int size) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Offer> farmerOffers = fetchFarmerOffersFromFirestoreOrMemory(farmerId);

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
     * Counter an active offer/proposal with atomic Firestore transaction safety.
     */
    public synchronized OfferResponse counterOffer(String callerUid, String offerId, CounterOfferRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Counter offer request cannot be null.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference offerRef = firestore.collection(COLLECTION_NAME).document(offerId);

                Offer updatedOffer = firestore.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(offerRef).get();
                    if (!snap.exists()) {
                        throw new NoSuchElementException("Offer not found with ID: " + offerId);
                    }

                    Offer offer = mapDocToOffer(snap);

                    checkAndApplyLazyExpiration(offer);

                    if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
                        throw new AccessDeniedException("Unauthorized access to offer.");
                    }

                    if (!callerUid.equals(offer.getCurrentResponderUid())) {
                        throw new IllegalStateException("It is not your turn to respond to this negotiation round.");
                    }

                    if (isTerminalStatus(offer.getStatus())) {
                        throw new IllegalStateException("Cannot counter an offer in terminal state: " + offer.getStatus());
                    }

                    if (offer.getRoundNumber() >= maxNegotiationRounds) {
                        throw new IllegalArgumentException("Maximum negotiation rounds reached (" + maxNegotiationRounds + "). Further counter-offers are not allowed.");
                    }

                    ProductListingResponse listing = listingService.getListingById(offer.getListingId(), callerUid);
                    if (listing == null || !"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
                        throw new IllegalArgumentException("Cannot counter offer because the marketplace listing is no longer active.");
                    }

                    if (request.getCounterQuantity() == null || request.getCounterQuantity() <= 0) {
                        throw new IllegalArgumentException("Counter quantity must be greater than zero.");
                    }
                    if (listing.getAvailableQuantity() != null && request.getCounterQuantity() > listing.getAvailableQuantity()) {
                        throw new IllegalArgumentException("Counter quantity (" + request.getCounterQuantity() + ") cannot exceed available quantity (" + listing.getAvailableQuantity() + ").");
                    }

                    if (request.getCounterPrice() == null || request.getCounterPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Counter price must be greater than zero.");
                    }

                    String unit = (request.getQuantityUnit() != null) ? request.getQuantityUnit().trim().toUpperCase(Locale.ROOT) : offer.getQuantityUnit();
                    String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : offer.getPriceUnit();
                    String sanitizeMsg = sanitizeMessage(request.getMessage());

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
                            sanitizeMsg,
                            nowIso
                    );

                    offer.setOfferedQuantity(request.getCounterQuantity());
                    offer.setQuantityUnit(unit);
                    offer.setOfferedPrice(request.getCounterPrice());
                    offer.setPriceUnit(priceUnit);
                    offer.setMessage(sanitizeMsg);
                    offer.setStatus(OfferStatus.COUNTERED.name());
                    offer.setRoundNumber(nextRoundNumber);
                    offer.setCurrentResponderUid(nextResponderUid);
                    offer.getRounds().add(round);
                    offer.setUpdatedAt(nowIso);
                    offer.setRespondedAt(nowIso);

                    Map<String, Object> docMap = mapOfferToDoc(offer);
                    transaction.set(offerRef, docMap, SetOptions.merge());

                    return offer;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(updatedOffer, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to process counter-offer transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution for unit tests (when firestore is null)
        Offer offer = inMemoryOffers.get(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to respond to this negotiation round.");
        }

        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot counter an offer in terminal state: " + offer.getStatus());
        }

        if (offer.getRoundNumber() >= maxNegotiationRounds) {
            throw new IllegalArgumentException("Maximum negotiation rounds reached (" + maxNegotiationRounds + "). Further counter-offers are not allowed.");
        }

        ProductListingResponse listing = listingService.getListingById(offer.getListingId(), callerUid);
        if (listing == null || !"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Cannot counter offer because the marketplace listing is no longer active.");
        }

        if (request.getCounterQuantity() == null || request.getCounterQuantity() <= 0) {
            throw new IllegalArgumentException("Counter quantity must be greater than zero.");
        }
        if (listing.getAvailableQuantity() != null && request.getCounterQuantity() > listing.getAvailableQuantity()) {
            throw new IllegalArgumentException("Counter quantity (" + request.getCounterQuantity() + ") cannot exceed available quantity (" + listing.getAvailableQuantity() + ").");
        }

        if (request.getCounterPrice() == null || request.getCounterPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Counter price must be greater than zero.");
        }

        String unit = (request.getQuantityUnit() != null) ? request.getQuantityUnit().trim().toUpperCase(Locale.ROOT) : offer.getQuantityUnit();
        String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : offer.getPriceUnit();
        String sanitizeMsg = sanitizeMessage(request.getMessage());

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
                sanitizeMsg,
                nowIso
        );

        offer.setOfferedQuantity(request.getCounterQuantity());
        offer.setQuantityUnit(unit);
        offer.setOfferedPrice(request.getCounterPrice());
        offer.setPriceUnit(priceUnit);
        offer.setMessage(sanitizeMsg);
        offer.setStatus(OfferStatus.COUNTERED.name());
        offer.setRoundNumber(nextRoundNumber);
        offer.setCurrentResponderUid(nextResponderUid);
        offer.getRounds().add(round);
        offer.setUpdatedAt(nowIso);
        offer.setRespondedAt(nowIso);

        inMemoryOffers.put(offerId, offer);
        return toResponse(offer, callerUid);
    }

    /**
     * Accept an active proposal with atomic Firestore transaction safety.
     */
    public synchronized OfferResponse acceptOffer(String callerUid, String offerId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference offerRef = firestore.collection(COLLECTION_NAME).document(offerId);

                Offer updatedOffer = firestore.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(offerRef).get();
                    if (!snap.exists()) {
                        throw new NoSuchElementException("Offer not found with ID: " + offerId);
                    }

                    Offer offer = mapDocToOffer(snap);

                    checkAndApplyLazyExpiration(offer);

                    if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
                        throw new AccessDeniedException("Unauthorized access to offer.");
                    }

                    if (!callerUid.equals(offer.getCurrentResponderUid())) {
                        throw new IllegalStateException("It is not your turn to accept this proposal.");
                    }

                    if (isTerminalStatus(offer.getStatus())) {
                        throw new IllegalStateException("Cannot accept an offer in terminal state: " + offer.getStatus());
                    }

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

                    Map<String, Object> docMap = mapOfferToDoc(offer);
                    transaction.set(offerRef, docMap, SetOptions.merge());

                    return offer;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(updatedOffer, callerUid);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to process accept offer transaction: " + e.getMessage(), e);
            }
        }

        // In-memory fallback for unit tests
        Offer offer = inMemoryOffers.get(offerId);
        if (offer == null) {
            throw new NoSuchElementException("Offer not found with ID: " + offerId);
        }

        checkAndApplyLazyExpiration(offer);

        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to accept this proposal.");
        }

        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot accept an offer in terminal state: " + offer.getStatus());
        }

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

        inMemoryOffers.put(offerId, offer);
        return toResponse(offer, callerUid);
    }

    /**
     * Reject an active proposal.
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

        if (!callerUid.equals(offer.getBuyerUid()) && !callerUid.equals(offer.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to offer.");
        }

        if (!callerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("It is not your turn to reject this proposal.");
        }

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
        if (firestore == null) {
            inMemoryOffers.put(offerId, offer);
        }

        return toResponse(offer, callerUid);
    }

    /**
     * Cancel a pending/countered offer (initiator buyer only, when in PENDING or when buyer is the current responder).
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

        if (!buyerUid.equals(offer.getBuyerUid())) {
            throw new AccessDeniedException("Only the offer initiator can cancel this offer.");
        }

        if (isTerminalStatus(offer.getStatus())) {
            throw new IllegalStateException("Cannot cancel an offer in terminal state: " + offer.getStatus());
        }

        // Strict responder check for COUNTERED state cancellation
        if (OfferStatus.COUNTERED.name().equalsIgnoreCase(offer.getStatus()) && !buyerUid.equals(offer.getCurrentResponderUid())) {
            throw new IllegalStateException("Cannot cancel offer while waiting for the farmer to respond to your counter proposal.");
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
        if (firestore == null) {
            inMemoryOffers.put(offerId, offer);
        }

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

    private String sanitizeMessage(String msg) {
        if (msg == null) return null;
        String trimmed = msg.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            return trimmed.substring(0, MAX_MESSAGE_LENGTH);
        }
        return trimmed;
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
                    if (firestore == null) {
                        inMemoryOffers.put(offer.getOfferId(), offer);
                    }
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
        boolean canCounter = isParticipant && isResponder && activeState && (offer.getRoundNumber() < maxNegotiationRounds);
        boolean canCancel = isInitiator && activeState && (OfferStatus.PENDING.name().equalsIgnoreCase(offer.getStatus()) || isResponder);

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

    private Map<String, Object> mapOfferToDoc(Offer offer) {
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
        doc.put("offeredPrice", offer.getOfferedPrice() != null ? offer.getOfferedPrice().toString() : null);
        doc.put("priceUnit", offer.getPriceUnit());
        doc.put("askingPriceReference", offer.getAskingPriceReference() != null ? offer.getAskingPriceReference().toString() : null);
        doc.put("message", offer.getMessage());
        doc.put("status", offer.getStatus());
        doc.put("roundNumber", offer.getRoundNumber());
        doc.put("currentResponderUid", offer.getCurrentResponderUid());
        doc.put("agreedQuantity", offer.getAgreedQuantity());
        doc.put("agreedPrice", offer.getAgreedPrice() != null ? offer.getAgreedPrice().toString() : null);
        doc.put("agreedPriceUnit", offer.getAgreedPriceUnit());

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
                rMap.put("price", r.getPrice() != null ? r.getPrice().toString() : null);
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

        return doc;
    }

    private void saveOfferToFirestore(Offer offer) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Map<String, Object> doc = mapOfferToDoc(offer);
                firestore.collection(COLLECTION_NAME).document(offer.getOfferId()).set(doc, SetOptions.merge()).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Could not save offer {} to Firestore: {}", offer.getOfferId(), e.getMessage());
                throw new RuntimeException("Firestore error: " + e.getMessage(), e);
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
                return null;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error fetching offer {} from Firestore: {}", offerId, e.getMessage());
                throw new RuntimeException("Firestore error: " + e.getMessage(), e);
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
                logger.error("Error querying buyer offers from Firestore for buyer {}: {}", buyerUid, e.getMessage());
                throw new RuntimeException("Firestore error: " + e.getMessage(), e);
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
                logger.error("Error querying farmer offers from Firestore for farmer {}: {}", farmerId, e.getMessage());
                throw new RuntimeException("Firestore error: " + e.getMessage(), e);
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
                Object prcObj = rMap.get("price");
                BigDecimal prc = prcObj != null ? new BigDecimal(prcObj.toString()) : null;

                rounds.add(new OfferRound(
                        rNum != null ? rNum.intValue() : 1,
                        (String) rMap.get("senderUid"),
                        (String) rMap.get("senderRole"),
                        (String) rMap.get("action"),
                        qty != null ? qty.doubleValue() : null,
                        (String) rMap.get("quantityUnit"),
                        prc,
                        (String) rMap.get("priceUnit"),
                        (String) rMap.get("message"),
                        (String) rMap.get("timestamp")
                ));
            }
        }

        Object offPrcObj = doc.get("offeredPrice");
        BigDecimal offeredPrice = offPrcObj != null ? new BigDecimal(offPrcObj.toString()) : null;

        Object askPrcObj = doc.get("askingPriceReference");
        BigDecimal askingPriceRef = askPrcObj != null ? new BigDecimal(askPrcObj.toString()) : null;

        Object agrPrcObj = doc.get("agreedPrice");
        BigDecimal agreedPrice = agrPrcObj != null ? new BigDecimal(agrPrcObj.toString()) : null;

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
                offeredPrice,
                doc.getString("priceUnit"),
                askingPriceRef,
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
        offer.setAgreedPrice(agreedPrice);
        offer.setAgreedPriceUnit(doc.getString("agreedPriceUnit"));
        offer.setRespondedAt(doc.getString("respondedAt"));

        return offer;
    }
}
