package io.miraclecat.fbs.core.utils.web;

/**
 * packageName    : io.miraclecat.fbs.core.utils.web
 * fileName       : RedirectUtil
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : 로그인 후 이동 경로 검증 (open redirect 방지)
 */
public final class RedirectUtil {

    private RedirectUtil() {
    }

    /**
     * 내부 경로만 허용 (외부 URL, /auth 경로 차단)
     */
    public static boolean isSafe(String redirect) {
        return redirect != null
                && redirect.startsWith("/")
                && !redirect.startsWith("//")
                && !redirect.startsWith("/\\")
                && !redirect.startsWith("/auth");
    }

    public static String orDefault(String redirect) {
        return isSafe(redirect) ? redirect : "/";
    }
}
