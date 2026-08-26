package com.farmlink.api.controller;

import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.MarketPriceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/market-prices")
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    public MarketPriceController(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GetMapping
    public ResponseEntity<?> getMarketPrices(
            @RequestParam(required = false) String marketId,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String priceDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String qualityStatus,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer limit
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<MarketPriceSummaryResponse> prices = marketPriceService.getMarketPrices(
                marketId, cropId, priceDate, fromDate, toDate, qualityStatus, unit, state, district, limit
        );
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestMarketPrice(
            @RequestParam String marketId,
            @RequestParam String cropId,
            @RequestParam(required = false) String unit
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            MarketPriceResponse price = marketPriceService.getLatestMarketPrice(marketId, cropId);
            if (unit != null && !unit.trim().isEmpty()) {
                price = marketPriceService.applyConversion(price, unit.trim());
            }
            return ResponseEntity.ok(price);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getMarketPriceHistory(
            @RequestParam String marketId,
            @RequestParam String cropId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String unit
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            List<MarketPriceResponse> history = marketPriceService.getMarketPriceHistory(marketId, cropId, fromDate, toDate);
            if (unit != null && !unit.trim().isEmpty()) {
                String targetUnit = unit.trim();
                history = history.stream().map(p -> marketPriceService.applyConversion(p, targetUnit)).toList();
            }
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{priceId}")
    public ResponseEntity<?> getMarketPriceById(
            @PathVariable String priceId,
            @RequestParam(required = false) String unit
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            MarketPriceResponse price = marketPriceService.getMarketPriceById(priceId);
            if (unit != null && !unit.trim().isEmpty()) {
                price = marketPriceService.applyConversion(price, unit.trim());
            }
            return ResponseEntity.ok(price);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createMarketPrice() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Direct client creation of market prices is forbidden.");
    }

    @PutMapping("/{priceId}")
    public ResponseEntity<?> updateMarketPrice(@PathVariable String priceId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Direct client modification of market prices is forbidden.");
    }

    @DeleteMapping("/{priceId}")
    public ResponseEntity<?> deleteMarketPrice(@PathVariable String priceId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Direct client deletion of market prices is forbidden.");
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
