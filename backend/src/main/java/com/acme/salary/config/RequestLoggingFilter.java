package com.acme.salary.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator") || uri.startsWith("/h2-console") || uri.endsWith(".ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        long startTime = System.currentTimeMillis();
        String method = wrappedRequest.getMethod();
        String queryString = wrappedRequest.getQueryString() != null ? "?" + wrappedRequest.getQueryString() : "";
        String clientIp = wrappedRequest.getRemoteAddr();

        log.info("[HTTP IN]  {} {}{} from {}", method, uri, queryString, clientIp);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String payload = "";
            byte[] buf = wrappedRequest.getContentAsByteArray();
            if (buf.length > 0 && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
                int length = Math.min(buf.length, 1024);
                payload = " | payload=" + new String(buf, 0, length, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
            }

            if (status >= 400) {
                log.warn("[HTTP OUT] {} {}{} -> {} (took {}ms){}", method, uri, queryString, status, duration, payload);
            } else {
                log.info("[HTTP OUT] {} {}{} -> {} (took {}ms){}", method, uri, queryString, status, duration, payload);
            }
        }
    }
}
