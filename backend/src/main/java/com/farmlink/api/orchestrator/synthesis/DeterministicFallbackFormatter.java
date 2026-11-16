package com.farmlink.api.orchestrator.synthesis;

import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.router.dto.ClarificationDetails;
import com.farmlink.api.router.entity.AgriIntent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Generates structured, zero-hallucination GitHub Flavored Markdown responses
 * directly from grounded domain execution results when LLM is disabled or unavailable.
 */
@Component
public class DeterministicFallbackFormatter {

    @SuppressWarnings("unchecked")
    public String format(DomainExecutionResult result, String query) {
        if (result == null) {
            return "Unable to process agricultural query at this time. Please try again later.";
        }

        AgriIntent intent = result.intent();
        Map<String, Object> data = result.domainData();

        return switch (intent) {
            case MARKET_PRICE_INQUIRY -> formatMarketPrice(data, result.warnings());
            case MARKET_TREND_INQUIRY -> formatMarketTrend(data, result.warnings());
            case FARM_ECONOMICS_INQUIRY -> formatFarmEconomics(data, result.warnings());
            case MARKETPLACE_DISCOVERY -> formatMarketplace(data, result.warnings());
            case WEATHER_INQUIRY -> formatWeather(data);
            case PEST_MANAGEMENT, PLANT_DISEASE_INQUIRY -> formatDiagnostic(data, intent, result.warnings());
            case UNSUPPORTED_OR_UNCLEAR -> formatClarification(data);
            default -> formatAgronomic(data, intent, result.warnings());
        };
    }

    @SuppressWarnings("unchecked")
    private String formatMarketPrice(Map<String, Object> data, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        String cropName = (String) data.getOrDefault("cropName", "Commodity");
        String marketName = (String) data.getOrDefault("marketName", "Market");
        Map<String, Object> latest = (Map<String, Object>) data.get("latestPrice");

        if (latest != null) {
            sb.append("### 🌾 Verified Market Prices: ").append(cropName).append("\n\n");
            sb.append("| Commodity | Market | Modal Price | Min Price | Max Price | Date | Quality |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");
            sb.append(String.format("| **%s** | %s (%s) | **₹%.2f / %s** | ₹%.2f | ₹%.2f | %s | %s |\n\n",
                    cropName,
                    latest.getOrDefault("marketName", marketName),
                    latest.getOrDefault("state", "India"),
                    latest.get("modalPrice"),
                    latest.getOrDefault("priceUnit", "QUINTAL"),
                    latest.get("minPrice"),
                    latest.get("maxPrice"),
                    latest.get("priceDate"),
                    latest.getOrDefault("qualityStatus", "VERIFIED")
            ));
            sb.append("> **Note**: Real-time mandi rates sourced from verified agricultural arrival records.\n");
        } else {
            sb.append("### 🌾 Market Price Information: ").append(cropName).append("\n\n");
            sb.append("No verified market price records were found matching your query for ").append(cropName);
            if (marketName != null && !marketName.isBlank()) {
                sb.append(" in ").append(marketName);
            }
            sb.append(".\n\n");
            sb.append("> **Tip**: Check neighboring mandis or broaden your regional search.\n");
        }

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatMarketTrend(Map<String, Object> data, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        String cropName = (String) data.getOrDefault("cropName", "Commodity");
        Map<String, Object> trend = (Map<String, Object>) data.get("trend");

        sb.append("### 📈 Market Trend Analysis: ").append(cropName).append("\n\n");

        if (trend != null) {
            String dir = (String) trend.getOrDefault("trendDirection", "STABLE");
            Double pct = (Double) trend.getOrDefault("percentageChange", 0.0);
            Integer obsCount = (Integer) trend.getOrDefault("observationCount", 0);
            Double latestPrice = (Double) trend.get("latestPrice");
            String unit = (String) trend.getOrDefault("unit", "QUINTAL");

            String icon = switch (dir) {
                case "RISING" -> "📈";
                case "FALLING" -> "📉";
                default -> "➡️";
            };

            sb.append(String.format("- **Trend Direction**: %s **%s**\n", icon, dir));
            sb.append(String.format("- **30-Day Percentage Change**: %+.2f%%\n", pct));
            sb.append(String.format("- **Verified Observations Analyzed**: %d records\n", obsCount));
            if (latestPrice != null) {
                sb.append(String.format("- **Latest Verified Price**: ₹%.2f per %s\n", latestPrice, unit));
            }
            sb.append("\n");
        } else {
            sb.append("Historical price observations were insufficient to establish a reliable 30-day trend for ").append(cropName).append(".\n\n");
        }

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatFarmEconomics(Map<String, Object> data, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 Farm Economics & Break-Even Analysis\n\n");

        Map<String, Object> rec = (Map<String, Object>) data.get("economicRecord");
        if (rec != null) {
            sb.append(String.format("Economics budget for **%s** on farm **'%s'**:\n\n",
                    rec.get("cropName"), rec.get("farmName")));
            sb.append(String.format("- **Cultivated Area**: %s %s\n", rec.get("cultivatedArea"), rec.get("cultivatedAreaUnit")));
            sb.append(String.format("- **Expected Yield**: %s %s\n", rec.get("expectedYield"), rec.get("yieldUnit")));
            sb.append(String.format("- **Total Production Cost**: ₹%s\n", rec.get("totalProductionCost")));
            sb.append(String.format("- **Total Selling Cost**: ₹%s\n", rec.get("totalSellingCost")));
            sb.append(String.format("- **Total Cultivation Cost**: ₹%s\n", rec.get("totalCost")));
            sb.append(String.format("- **Cost of Production per %s**: ₹%s\n", rec.get("yieldUnit"), rec.get("totalCostPerUnit")));
            sb.append(String.format("- **Break-Even Selling Price**: **₹%s per %s**\n\n", rec.get("breakEvenSellingPrice"), rec.get("yieldUnit")));

            if (data.containsKey("currentMarketPrice") && data.containsKey("projectedMarginPerUnit")) {
                Double modal = (Double) data.get("currentMarketPrice");
                Double margin = (Double) data.get("projectedMarginPerUnit");
                Double marginPct = (Double) data.get("projectedMarginPercent");
                String badge = margin >= 0 ? "🟢 PROFITABLE" : "🔴 AT LOSS";
                sb.append(String.format("> **Market Benchmark**: Current verified modal price is ₹%.2f per %s. " +
                                "Projected margin: **₹%+.2f (%+.1f%%)** [%s].\n\n",
                        modal, rec.get("yieldUnit"), margin, marginPct, badge));
            }
        } else {
            sb.append("No active farm economic records were found for your registered farms.\n\n");
            sb.append("> **Tip**: Use Farm Management to add production costs (seeds, fertilizers, labor) to track break-even metrics.\n");
        }

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatMarketplace(Map<String, Object> data, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        String crop = (String) data.getOrDefault("targetCrop", "Commodities");
        Long total = (Long) data.getOrDefault("totalActiveListings", 0L);
        List<Map<String, Object>> listings = (List<Map<String, Object>>) data.get("listings");

        sb.append("### 🛒 Marketplace Discovery: ").append(crop).append("\n\n");

        if (listings != null && !listings.isEmpty()) {
            sb.append(String.format("Displaying top %d of %d active marketplace listings:\n\n", listings.size(), total));
            for (Map<String, Object> item : listings) {
                sb.append(String.format("- **%s** (%s) — **₹%.2f / %s** | Available: %.1f %s | Location: %s, %s\n",
                        item.get("cropName"),
                        item.getOrDefault("qualityGrade", "STANDARD"),
                        item.get("askingPrice"),
                        item.getOrDefault("priceUnit", "QUINTAL"),
                        item.get("availableQuantity"),
                        item.getOrDefault("quantityUnit", "QUINTAL"),
                        item.getOrDefault("district", "Regional"),
                        item.getOrDefault("state", "India")
                ));
            }
            sb.append("\n> **Note**: Listings represent active farmer supplies. Zero order or transaction operations are executed from chat.\n");
        } else {
            sb.append("No active marketplace listings currently found for ").append(crop).append(".\n\n");
        }

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    private String formatWeather(Map<String, Object> data) {
        return "### 🌦️ Agro-Meteorological Advisory\n\n" +
                data.getOrDefault("message", "Real-time weather telemetry is not integrated.") + "\n\n" +
                "🔗 **Official Portal**: [India Meteorological Department (IMD) Mausam](https://mausam.imd.gov.in)\n";
    }

    @SuppressWarnings("unchecked")
    private String formatDiagnostic(Map<String, Object> data, AgriIntent intent, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        String title = intent == AgriIntent.PEST_MANAGEMENT ? "Pest Management & IPM" : "Plant Disease Identification";
        String crop = (String) data.getOrDefault("cropName", "Crop");

        sb.append("### 🛡️ ").append(title).append(": ").append(crop).append("\n\n");

        List<Map<String, Object>> priorHistory = (List<Map<String, Object>>) data.get("farmerPriorDiagnostics");
        if (priorHistory != null && !priorHistory.isEmpty()) {
            sb.append("**Your Prior Diagnostic Scans for ").append(crop).append("**:\n");
            for (Map<String, Object> h : priorHistory) {
                sb.append(String.format("- Reported %s on %s (Status: %s)\n",
                        h.getOrDefault("symptoms", "Symptoms"),
                        h.getOrDefault("createdAt", "Recent"),
                        h.getOrDefault("status", "PENDING")));
            }
            sb.append("\n");
        }

        sb.append("**Integrated Pest & Disease Management Principles**:\n");
        sb.append("- Regularly inspect the underside of leaves for early symptom emergence.\n");
        sb.append("- Adopt cultural sanitization: remove and safely discard severely infected plant debris.\n");
        sb.append("- Utilize sticky traps, pheromone traps, or neem-based organic bio-repellents as first-line defense.\n\n");

        sb.append("> 📷 **Photo Diagnosis**: ").append(data.getOrDefault("advisoryNote", "")).append("\n\n");
        sb.append("> ⚠️ **Extension Disclaimer**: ").append(data.getOrDefault("disclaimer", "")).append("\n");

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    private String formatAgronomic(Map<String, Object> data, AgriIntent intent, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        String crop = (String) data.getOrDefault("cropName", "Crop");
        String sci = (String) data.getOrDefault("scientificName", "");
        String cat = (String) data.getOrDefault("category", "Commodity");

        String intentTitle = intent.name().toLowerCase().replace('_', ' ');
        sb.append("### 🌿 Agronomic Guidelines: ").append(crop).append(" (").append(intentTitle).append(")\n\n");
        sb.append(String.format("- **Commodity**: **%s**\n", crop));
        if (!sci.isBlank()) {
            sb.append(String.format("- **Botanical Name**: *%s*\n", sci));
        }
        sb.append(String.format("- **Category**: %s\n\n", cat));

        if (intent == AgriIntent.CROP_SELECTION && data.containsKey("forecastPrice")) {
            sb.append(String.format("**Market Forecast**: 7-day projected price is ₹%.2f (%s, confidence: %s).\n\n",
                    data.get("forecastPrice"),
                    data.getOrDefault("forecastDirection", "STABLE"),
                    data.getOrDefault("forecastConfidence", "MEDIUM")));
        }

        sb.append("**Standard Extension Best Practices**:\n");
        sb.append("- Follow regional agro-climatic seasonal calendars for land preparation and optimal sowing windows.\n");
        sb.append("- Implement soil conservation, mulching, and balanced organic manure incorporation prior to sowing.\n");
        sb.append("- Practice micro-irrigation (drip/sprinkler) at critical crop growth stages to optimize water use efficiency.\n\n");

        sb.append("> ⚠️ **Advisory Notice**: ").append(data.getOrDefault("disclaimer", "")).append("\n");

        appendWarnings(sb, warnings);
        return sb.toString();
    }

    private String formatClarification(Map<String, Object> data) {
        Object clarObj = data.get("clarification");
        if (clarObj instanceof ClarificationDetails clar && clar.required()) {
            StringBuilder sb = new StringBuilder();
            sb.append("### ❓ Clarification Needed\n\n");
            sb.append(clar.prompt() != null ? clar.prompt() : "Please clarify your agricultural inquiry.").append("\n\n");

            if (clar.candidateIntents() != null && !clar.candidateIntents().isEmpty()) {
                sb.append("**Available Options**:\n");
                for (AgriIntent opt : clar.candidateIntents()) {
                    sb.append("- ").append(opt.name().toLowerCase().replace('_', ' ')).append("\n");
                }
            }
            if (clar.missingEntities() != null && !clar.missingEntities().isEmpty()) {
                sb.append("\n**Missing Details**: ").append(String.join(", ", clar.missingEntities())).append("\n");
            }
            return sb.toString();
        }
        return "Please provide more details regarding the specific crop, market, or agricultural topic you need assistance with.";
    }

    private void appendWarnings(StringBuilder sb, List<String> warnings) {
        if (warnings != null && !warnings.isEmpty()) {
            sb.append("\n---\n*Notice*: ");
            for (String w : warnings) {
                sb.append(w).append(" ");
            }
            sb.append("\n");
        }
    }
}
