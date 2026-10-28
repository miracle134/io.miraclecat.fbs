package io.miraclecat.fbs.core.utils.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseCookie;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;

/**
 * packageName    : io.miraclecat.fbs.auth.utils
 * fileName       : SessionAndCookieUtil
 * author         : MiracleCat
 * date           : 2024-05-02
 * description    :
 */
public class SessionAndCookieUtil {

    /**
     * HttpSession에 주어진 키 값으로 세션 정보를 생성하는 기능
     *
     * @param request
     * @param keyStr - 세션 키
     * @param valStr - 세션 값
     * @throws Exception
     */
    public static void setSessionAttribute(HttpServletRequest request, String keyStr, String valStr) throws Exception {

        HttpSession session = request.getSession();
        session.setAttribute(keyStr, valStr);
    }

    /**
     * HttpSession에 주어진 키 값으로 세션 객체를 생성하는 기능
     *
     * @param request
     * @param keyStr - 세션 키
     * @param obj - 세션 값
     * @throws Exception
     */
    public static void setSessionAttribute(HttpServletRequest request, String keyStr, Object obj) throws Exception {

        HttpSession session = request.getSession();
        session.setAttribute(keyStr, obj);
    }

    /**
     * HttpSession에 존재하는 주어진 키 값에 해당하는 세션 값을 얻어오는 기능
     *
     * @param request
     * @param keyStr - 세션 키
     * @return
     * @throws Exception
     */
    public static Object getSessionAttribute(HttpServletRequest request, String keyStr) throws Exception {

        HttpSession session = request.getSession();
        return session.getAttribute(keyStr);
    }

    /**
     * HttpSession 객체내의 모든 값을 호출하는 기능
     *
     * @param request
     * @return
     * @throws Exception
     */
    public static String getSessionValuesString(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        String returnVal = "";

        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            String sessionKey = (String)e.nextElement();
            returnVal = returnVal + "[" + sessionKey + " : " + session.getAttribute(sessionKey) + "]";
        }

        return returnVal;
    }

    /**
     * HttpSession 존재하는 세션을 주어진 키 값으로 삭제하는 기능
     *
     * @param request - request
     * @param keyStr - 세션 키
     * @throws Exception
     */
    public static void removeSessionAttribute(HttpServletRequest request, String keyStr) throws Exception {

        HttpSession session = request.getSession();
        session.removeAttribute(keyStr);
    }
    
    /**
     * 쿠키생성 - 입력받은 시간 만큼 쿠키를 유지되도록 세팅한다.
     * 쿠키의 유효시간을 5분으로 설정 =>(cookie.setMaxAge(60 * 5)
     * 쿠키의 유효시간을 5시간으로 설정 =>(cookie.setMaxAge(60 * 60 * 5)
     * 쿠키의 유효시간을 10일로 설정 =>(cookie.setMaxAge(60 * 60 * 24 * 10)
     *
     * @param response - response
     * @param cookieNm - 쿠키명
     * @param cookieValue - 쿠키값
     * @param time - 시간 (초단위)
     */
    public static void setCookie(HttpServletResponse response, String cookieNm, String cookieValue, int time) throws UnsupportedEncodingException {
        Cookie cookie = new Cookie(cookieNm, URLEncoder.encode(cookieValue, "utf-8"));
        cookie.setPath("/");
        cookie.setMaxAge(time);
        response.addCookie(cookie);
    }
    
    /**
     * 쿠키생성 - 입력받은 시간 만큼 쿠키를 유지되도록 세팅한다.
     * 쿠키의 유효시간을 5분으로 설정 =>(cookie.setMaxAge(60 * 5)
     * 쿠키의 유효시간을 5시간으로 설정 =>(cookie.setMaxAge(60 * 60 * 5)
     * 쿠키의 유효시간을 10일로 설정 =>(cookie.setMaxAge(60 * 60 * 24 * 10)
     *
     * @param response - response
     * @param cookieNm - 쿠키명
     * @param cookieValue - 쿠키값
     * @param httpOnly - javascript 접근불가 여부 : true(접근 불가)
     * @param time - 시간 (초단위)
     */
    public static void setCookie(HttpServletResponse response, String cookieNm, String cookieValue, int time, boolean httpOnly) throws UnsupportedEncodingException {
        ResponseCookie cookie = ResponseCookie.from(cookieNm, URLEncoder.encode(cookieValue, "utf-8"))
            .path("/")
            .maxAge(time)
            .httpOnly(httpOnly)
            .sameSite("Lax")
            .build();
        
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 쿠키생성 - HttpOnly / Secure 지정 (SameSite=Lax)
     *
     * @param response - response
     * @param cookieNm - 쿠키명
     * @param cookieValue - 쿠키값
     * @param time - 시간 (초단위)
     * @param httpOnly - javascript 접근불가 여부
     * @param secure - HTTPS 에서만 전송 여부
     */
    public static void setCookie(HttpServletResponse response, String cookieNm, String cookieValue, int time,
                                 boolean httpOnly, boolean secure) throws UnsupportedEncodingException {
        ResponseCookie cookie = ResponseCookie.from(cookieNm, URLEncoder.encode(cookieValue, "utf-8"))
            .path("/")
            .maxAge(time)
            .httpOnly(httpOnly)
            .secure(secure)
            .sameSite("Lax")
            .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 쿠키생성 - 쿠키의 유효시간을 설정하지 않을 경우 쿠키의 생명주기는 브라우저가 종료될 때까지
     *
     * @param response - Response
     * @param cookieNm - 쿠키명
     * @param cookieValue - 쿠키값
     * @param httpOnly - javascript 접근불가 여부 : true(접근 불가)
     */
    public static void setCookie(HttpServletResponse response, String cookieNm, String cookieValue, boolean httpOnly) throws UnsupportedEncodingException {
        ResponseCookie cookie = ResponseCookie.from(cookieNm, URLEncoder.encode(cookieValue, "utf-8"))
            .path("/")
            .httpOnly(httpOnly)
            .sameSite("Lax")
            .build();
            
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 쿠키값 사용 - 쿠키값을 읽어들인다.
     *
     * @param request - request
     * @param cookieNm - 쿠키명
     * @return 쿠키값
     * @throws Exception
     */
    public static String getCookie(HttpServletRequest request, String cookieNm) throws UnsupportedEncodingException {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) { // 빈 쿠키
            return "";
        }

        String cookieValue = null;

        for (Cookie cookie : cookies) {
            if (cookieNm.equals(cookie.getName())) {
                cookieValue = URLDecoder.decode(cookie.getValue(), "utf-8");
                break;
            }
        }

        return cookieValue;
    }

    /**
     * 쿠키값 삭제 - cookie.setMaxAge(0) - 쿠키의 유효시간을 0으로 설정해 줌으로써 쿠키를 삭제하는 것과 동일한 효과
     *
     * @param response
     * @param cookieNm 쿠키명
     * @throws UnsupportedEncodingException
     */
    public static void removeCookie(HttpServletResponse response, String cookieNm) {
        Cookie cookie = new Cookie(cookieNm, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}