package io.miraclecat.fbs.core.audit.authentication.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * packageName    : io.miraclecat.fbs.core.audit.authentication.util
 * fileName       : AuthenticationLogUtil
 * author         : MiracleCat
 * date           : 2025-09-23
 * description    :
 */
@Slf4j
public class AuthenticationLogUtil {


    // IPv4 우선 IP 추출
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;

        String ip = null;

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            ip = getPreferredIp(xForwardedFor);
        }

        if (ip == null || ip.isEmpty()) {
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                ip = getPreferredIp(xRealIp);
            }
        }

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    // IPv4 우선 선택
    private static String getPreferredIp(String ips) {
        String[] ipArray = ips.split(",");

        // 1순위: IPv4 찾기
        for (String ip : ipArray) {
            ip = ip.trim();
            if (isIPv4(ip)) {
                return ip;
            }
        }

        // 2순위: IPv4 없으면 첫 번째 IP 반환
        return ipArray[0].trim();
    }

    // IPv4 체크
    private static boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        // IPv6 형태면 false (콜론 포함)
        if (ip.contains(":")) return false;

        // IPv4 패턴 체크
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 메시지 길이 제한
    public static  String truncateMessage(String message, int maxLength) {
        if (message == null) return null;
        if (message.length() <= maxLength) return message;

        return message.substring(0, maxLength - 3) + "...";
    }

}