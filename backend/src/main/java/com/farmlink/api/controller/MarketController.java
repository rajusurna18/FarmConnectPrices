package com.farmlink.api.controller;

import com.farmlink.api.dto.MarketCropResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.MarketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/markets")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping
    public ResponseEntity<?> getMarkets(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) Integer limit
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<MarketSummaryResponse> markets = marketService.getMarkets(state, district, mandal, type, status, cropId, limit);
        return ResponseEntity.ok(markets);
    }

    @GetMapping("/{marketId}")
    public ResponseEntity<?> getMarketById(@PathVariable String marketId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            MarketResponse market = marketService.getMarketById(marketId);
            return ResponseEntity.ok(market);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{marketId}/crops")
    public ResponseEntity<?> getMarketCrops(@PathVariable String marketId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            // Verifies market exists
            marketService.getMarketById(marketId);
            List<MarketCropResponse> crops = marketService.getMarketCrops(marketId);
            return ResponseEntity.ok(crops);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createMarket() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Master market creation is forbidden.");
    }

    @PutMapping("/{marketId}")
    public ResponseEntity<?> updateMarket(@PathVariable String marketId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Master market update is forbidden.");
    }

    @DeleteMapping("/{marketId}")
    public ResponseEntity<?> deleteMarket(@PathVariable String marketId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Master market deletion is forbidden.");
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
