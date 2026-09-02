package com.cristiannustes.http;

public final class MediaType {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String LOCATION = "Location";
    public static final String ALLOW = "Allow";
    public static final String REQUEST_ID = "X-Request-Id";

    public static final String APPLICATION_JSON = "application/json; charset=utf-8";

    private MediaType() {
    }

    public static boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        int parameterStart = contentType.indexOf(';');
        String base = parameterStart < 0 ? contentType : contentType.substring(0, parameterStart);
        return base.trim().equalsIgnoreCase("application/json");
    }
}
