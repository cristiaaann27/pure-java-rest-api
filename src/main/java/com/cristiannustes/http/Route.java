package com.cristiannustes.http;

record Route(HttpMethod method, PathPattern pattern, RouteHandler handler) {
}
