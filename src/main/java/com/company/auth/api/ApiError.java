package com.company.auth.api;

import java.util.Map;

public class ApiError {
    public String code;
    public String message;
    public String traceId;
    public Map<String, Object> details;

    public ApiError(String code, String message, String traceId, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.details = details;
    }
}
