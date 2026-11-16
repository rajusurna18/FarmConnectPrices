package com.farmlink.api.router.idempotency;

import com.farmlink.api.router.exception.InvalidQueryException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;

/**
 * Calculates deterministic SHA-256 canonical fingerprints for router requests.
 */
@Component
public class CanonicalFingerprintCalculator {

    public static final int MAX_CODE_POINTS = 1000;
    public static final int MAX_NORMALIZED_QUERY_BYTES = 4096;
    public static final long MIN_TURN_INDEX = 1L;
    public static final long MAX_TURN_INDEX = 2147483646L;

    public String calculateFingerprint(String ownerUserId, long turnIndex, String rawQuery) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
            throw new InvalidQueryException("ownerUserId must not be null or blank");
        }
        if (turnIndex < MIN_TURN_INDEX || turnIndex > MAX_TURN_INDEX) {
            throw new InvalidQueryException("turnIndex out of bounds: " + turnIndex);
        }
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new InvalidQueryException("query must not be null or blank");
        }

        // Code point validation
        int codePointCount = rawQuery.codePointCount(0, rawQuery.length());
        if (codePointCount > MAX_CODE_POINTS) {
            throw new InvalidQueryException("query exceeds maximum allowed code points (" + MAX_CODE_POINTS + "): " + codePointCount);
        }

        // NFC normalization & whitespace strip
        String normalizedQuery = Normalizer.normalize(rawQuery, Normalizer.Form.NFC).strip();
        if (normalizedQuery.isEmpty()) {
            throw new InvalidQueryException("query must not be blank after normalization");
        }

        // Strict UTF-8 encoding with surrogate rejection
        byte[] queryBytes;
        try {
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            ByteBuffer buf = encoder.encode(CharBuffer.wrap(normalizedQuery));
            queryBytes = new byte[buf.remaining()];
            buf.get(queryBytes);
        } catch (CharacterCodingException e) {
            throw new InvalidQueryException("query contains invalid or unmappable UTF-8 characters: " + e.getMessage());
        }

        if (queryBytes.length > MAX_NORMALIZED_QUERY_BYTES) {
            throw new InvalidQueryException("normalized query exceeds maximum allowed bytes (" + MAX_NORMALIZED_QUERY_BYTES + "): " + queryBytes.length);
        }

        byte[] userBytes = ownerUserId.getBytes(StandardCharsets.UTF_8);
        byte[] turnBytes = Long.toString(turnIndex).getBytes(StandardCharsets.UTF_8);

        // Binary framing (Pascal length-prefixed big-endian)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(userBytes.length);
            dos.write(userBytes);

            dos.writeInt(turnBytes.length);
            dos.write(turnBytes);

            dos.writeInt(queryBytes.length);
            dos.write(queryBytes);
            dos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize canonical binary frame", e);
        }

        byte[] frame = baos.toByteArray();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(frame);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

