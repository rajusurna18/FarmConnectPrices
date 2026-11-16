package com.farmlink.api.router.extractor;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.service.CropMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CropEntityExtractor implements AgriculturalEntityExtractor<String> {

    private static final Logger logger = LoggerFactory.getLogger(CropEntityExtractor.class);

    private static final List<String> CANONICAL_CROPS = List.of(
            "paddy", "rice", "wheat", "cotton", "chilli", "mirchi", "turmeric", "haldi",
            "maize", "corn", "sugarcane", "soybean", "soya", "onion", "pyaz", "potato",
            "aloo", "tomato", "tamatar", "mustard", "sarson", "groundnut", "peanut",
            "gram", "chana", "red gram", "tur", "arhar", "green gram", "moong",
            "black gram", "urad", "bajra", "pearl millet", "jowar", "sorghum",
            "mango", "banana", "coffee", "tea", "jute", "sunflower", "garlic",
            "ginger", "barley", "cardamom", "pepper", "coriander", "cumin"
    );

    private final CropMasterService cropMasterService;

    public CropEntityExtractor(@Autowired(required = false) CropMasterService cropMasterService) {
        this.cropMasterService = cropMasterService;
    }

    @Override
    public List<String> extract(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> knownCrops = new LinkedHashSet<>(CANONICAL_CROPS);
        if (cropMasterService != null) {
            try {
                List<CropResponse> dbCrops = cropMasterService.getAllCrops();
                if (dbCrops != null) {
                    for (CropResponse c : dbCrops) {
                        if (c.getName() != null && !c.getName().isBlank()) {
                            knownCrops.add(c.getName().toLowerCase().trim());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not fetch crops from CropMasterService: {}", e.getMessage());
            }
        }

        List<String> matched = new ArrayList<>();
        // Sort crops descending by length so multi-word crops match before single-word substrings
        List<String> sortedCrops = new ArrayList<>(knownCrops);
        sortedCrops.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String crop : sortedCrops) {
            // Word boundary match case-insensitive
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(crop) + "\\b");
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                if (!matched.contains(crop)) {
                    matched.add(crop);
                }
            }
        }

        return Collections.unmodifiableList(matched);
    }
}
