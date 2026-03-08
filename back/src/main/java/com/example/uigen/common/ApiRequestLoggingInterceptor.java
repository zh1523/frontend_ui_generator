package com.example.uigen.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

@Component
public class ApiRequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingInterceptor.class);
    private static final String ATTR_START = "req_log_start";
    private static final String ATTR_REQUEST_ID = "req_log_request_id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        String requestId = extractRequestId(request);
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long start = getStart(request);
        long cost = Math.max(0, System.currentTimeMillis() - start);
        String requestId = getRequestId(request);
        String workspaceKey = maskWorkspaceKey(request.getHeader(HttpHeadersConst.WORKSPACE_KEY));
        String clientIp = getClientIp(request);
        String taskId = getPathVar(request, "taskId");
        String versionId = getPathVar(request, "versionId");
        int status = response.getStatus();
        String errorType = ex == null ? "-" : ex.getClass().getSimpleName();

        log.info("api reqId={} method={} uri={} status={} costMs={} ip={} workspace={} taskId={} versionId={} error={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                status,
                cost,
                clientIp,
                workspaceKey,
                taskId,
                versionId,
                errorType);
    }

    private String extractRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private long getStart(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_START);
        if (value instanceof Long start) {
            return start;
        }
        return System.currentTimeMillis();
    }

    private String getRequestId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_REQUEST_ID);
        if (value instanceof String id) {
            return id;
        }
        return "-";
    }

    @SuppressWarnings("unchecked")
    private String getPathVar(HttpServletRequest request, String key) {
        Object varsObj = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (varsObj instanceof Map<?, ?> vars) {
            Object val = vars.get(key);
            if (val != null) {
                return String.valueOf(val);
            }
        }
        return "-";
    }

    private String maskWorkspaceKey(String workspaceKey) {
        if (workspaceKey == null || workspaceKey.isBlank()) {
            return "-";
        }
        String key = workspaceKey.trim();
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
