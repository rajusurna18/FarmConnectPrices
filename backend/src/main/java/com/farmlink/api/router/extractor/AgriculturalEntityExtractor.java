package com.farmlink.api.router.extractor;

import java.util.List;

public interface AgriculturalEntityExtractor<T> {
    List<T> extract(String query);
}

