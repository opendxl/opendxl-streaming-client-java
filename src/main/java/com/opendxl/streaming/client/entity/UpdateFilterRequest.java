package com.opendxl.streaming.client.entity;

import java.util.HashMap;
import java.util.Map;

public class UpdateFilterRequest {

    private Map<String, Map<String, Object>> filter = new HashMap<String, Map<String, Object>>();

    public UpdateFilterRequest(Map<String, Map<String, Object>> filter) {
        this.filter = filter;
    }

    public Map<String, Map<String, Object>> getFilter() {
        return filter;
    }

}