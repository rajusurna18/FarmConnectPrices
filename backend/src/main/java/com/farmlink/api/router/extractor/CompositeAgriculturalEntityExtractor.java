package com.farmlink.api.router.extractor;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompositeAgriculturalEntityExtractor {

    private final CropEntityExtractor cropExtractor;
    private final LocationEntityExtractor locationExtractor;
    private final MandiEntityExtractor mandiExtractor;
    private final QuantityEntityExtractor quantityExtractor;

    public CompositeAgriculturalEntityExtractor(
            CropEntityExtractor cropExtractor,
            LocationEntityExtractor locationExtractor,
            MandiEntityExtractor mandiExtractor,
            QuantityEntityExtractor quantityExtractor) {
        this.cropExtractor = cropExtractor;
        this.locationExtractor = locationExtractor;
        this.mandiExtractor = mandiExtractor;
        this.quantityExtractor = quantityExtractor;
    }

    public ExtractedEntitiesDto extractEntities(String query) {
        if (query == null || query.isBlank()) {
            return ExtractedEntitiesDto.empty();
        }

        List<String> crops = cropExtractor.extract(query);
        List<String> locations = locationExtractor.extract(query);
        List<String> mandis = mandiExtractor.extract(query);
        List<String> quantities = quantityExtractor.extract(query);

        Map<String, Object> attributes = new HashMap<>();
        if (!crops.isEmpty()) {
            attributes.put("primaryCrop", crops.get(0));
        }
        if (!locations.isEmpty()) {
            attributes.put("primaryLocation", locations.get(0));
        }
        if (!mandis.isEmpty()) {
            attributes.put("primaryMandi", mandis.get(0));
        }

        return new ExtractedEntitiesDto(
                crops,
                locations,
                mandis,
                quantities,
                Collections.unmodifiableMap(attributes)
        );
    }
}

