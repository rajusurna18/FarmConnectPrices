package com.farmlink.api.router.extractor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuantityEntityExtractor implements AgriculturalEntityExtractor<String> {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(quintals?|qtls?|kgs?|kilograms?|tonnes?|tons?|acres?|hectares?|bags?|packets?|bighas?|gunthas?)\\b"
    );

    @Override
    public List<String> extract(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<String> matched = new ArrayList<>();
        Matcher matcher = QUANTITY_PATTERN.matcher(query);
        while (matcher.find()) {
            String value = matcher.group(1);
            String unit = matcher.group(2).toLowerCase();
            String quantityStr = value + " " + unit;
            if (!matched.contains(quantityStr)) {
                matched.add(quantityStr);
            }
        }

        return Collections.unmodifiableList(matched);
    }
}

