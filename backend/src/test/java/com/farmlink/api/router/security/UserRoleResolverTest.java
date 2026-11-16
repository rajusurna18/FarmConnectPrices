package com.farmlink.api.router.security;

import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.service.FirestoreQuotaGuard;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserRoleResolverTest {

    private Firestore firestore;
    private FirestoreQuotaGuard quotaGuard;
    private CollectionReference usersCollection;
    private DocumentReference userDocRef;
    private DocumentSnapshot snapshot;
    private UserRoleResolver resolver;

    @BeforeEach
    void setUp() {
        firestore = mock(Firestore.class);
        quotaGuard = mock(FirestoreQuotaGuard.class);
        usersCollection = mock(CollectionReference.class);
        userDocRef = mock(DocumentReference.class);
        snapshot = mock(DocumentSnapshot.class);
        resolver = new UserRoleResolver(firestore, quotaGuard);
    }

    @Test
    @DisplayName("Resolves FARMER role directly from Firestore")
    void testResolveFarmerRole() {
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("farmer1")).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getString("role")).thenReturn("FARMER");

        UserDomainRole role = resolver.resolveUserRole("farmer1");

        assertEquals(UserDomainRole.FARMER, role);
        verify(quotaGuard).checkQuotaAvailability();
        verify(quotaGuard).recordSuccess();
    }

    @Test
    @DisplayName("Resolves MEDIATOR_BUYER role directly from Firestore")
    void testResolveMediatorRole() {
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("buyer1")).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getString("role")).thenReturn("MEDIATOR_BUYER");

        UserDomainRole role = resolver.resolveUserRole("buyer1");

        assertEquals(UserDomainRole.MEDIATOR_BUYER, role);
        verify(quotaGuard).checkQuotaAvailability();
    }

    @Test
    @DisplayName("Non-existent user document defaults to CUSTOMER")
    void testNonExistentUserDefaultsToCustomer() {
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("unknown")).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(snapshot.exists()).thenReturn(false);

        UserDomainRole role = resolver.resolveUserRole("unknown");

        assertEquals(UserDomainRole.CUSTOMER, role);
        verify(quotaGuard).checkQuotaAvailability();
    }

    @Test
    @DisplayName("Circuit breaker open throws FirestoreQuotaExhaustedException")
    void testCircuitBreakerOpenThrowsException() {
        doThrow(new FirestoreQuotaExhaustedException("Circuit open"))
                .when(quotaGuard).checkQuotaAvailability();

        assertThrows(FirestoreQuotaExhaustedException.class, () ->
                resolver.resolveUserRole("farmer1")
        );
        verifyNoInteractions(firestore);
    }

    @Test
    @DisplayName("Null or blank UID defaults to CUSTOMER without Firestore query")
    void testNullOrBlankUid() {
        assertEquals(UserDomainRole.CUSTOMER, resolver.resolveUserRole(null));
        assertEquals(UserDomainRole.CUSTOMER, resolver.resolveUserRole("   "));
        verifyNoInteractions(quotaGuard);
        verifyNoInteractions(firestore);
    }
}
