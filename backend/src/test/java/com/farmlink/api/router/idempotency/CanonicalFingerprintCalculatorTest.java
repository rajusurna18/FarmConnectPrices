package com.farmlink.api.router.idempotency;

import com.farmlink.api.router.exception.InvalidQueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalFingerprintCalculatorTest {

    private CanonicalFingerprintCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CanonicalFingerprintCalculator();
    }

    @Test
    @DisplayName("Valid query generates deterministic 64-character lowercase hex SHA-256 fingerprint")
    void testValidFingerprintGeneration() {
        String fp1 = calculator.calculateFingerprint("user123", 1L, "What is the price of tomato in Guntur?");
        String fp2 = calculator.calculateFingerprint("user123", 1L, "What is the price of tomato in Guntur?");

        assertNotNull(fp1);
        assertEquals(64, fp1.length());
        assertTrue(fp1.matches("^[a-f0-9]+$"));
        assertEquals(fp1, fp2, "Fingerprint must be deterministic");
    }

    @Test
    @DisplayName("NFC normalization and whitespace stripping produce identical fingerprints")
    void testNfcNormalizationAndWhitespace() {
        String fp1 = calculator.calculateFingerprint("user123", 1L, "tomato price");
        String fp2 = calculator.calculateFingerprint("user123", 1L, "  tomato price  \t");

        assertEquals(fp1, fp2, "Normalized queries with leading/trailing whitespace must match");
    }

    @Test
    @DisplayName("Different users or turns produce distinct fingerprints")
    void testDistinctFingerprints() {
        String fp1 = calculator.calculateFingerprint("user123", 1L, "tomato price");
        String fp2 = calculator.calculateFingerprint("user456", 1L, "tomato price");
        String fp3 = calculator.calculateFingerprint("user123", 2L, "tomato price");

        assertNotEquals(fp1, fp2);
        assertNotEquals(fp1, fp3);
    }

    @Test
    @DisplayName("Code points exceeding 1000 throw InvalidQueryException")
    void testQueryExceedsMaxCodePoints() {
        String largeQuery = "a".repeat(1001);
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("user123", 1L, largeQuery)
        );
    }

    @Test
    @DisplayName("Blank query or blank ownerUserId throws InvalidQueryException")
    void testBlankInputsThrowException() {
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("user123", 1L, "   ")
        );
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("", 1L, "valid query")
        );
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint(null, 1L, "valid query")
        );
    }

    @Test
    @DisplayName("Turn index out of bounds throws InvalidQueryException")
    void testTurnIndexOutOfBounds() {
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("user123", 0L, "valid query")
        );
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("user123", -1L, "valid query")
        );
        assertThrows(InvalidQueryException.class, () ->
                calculator.calculateFingerprint("user123", 2147483647L, "valid query")
        );
    }
}

