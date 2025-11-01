package org.example.cache;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * A very small Map view wrapper around PrehashCache that implements Map<String,String>.
 * This gives App.java the Map<String,String> it expects.
 */
public class PrehashMapView extends AbstractMap<String, String> {

    private final PrehashCache cache;

    public PrehashMapView(PrehashCache cache) {
        this.cache = cache;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return cache.asMap().entrySet();
    }

    @Override
    public String get(Object key) {
        return cache.asMap().get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.asMap().containsKey(key);
    }
}
