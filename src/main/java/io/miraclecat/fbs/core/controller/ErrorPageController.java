package io.miraclecat.fbs.core.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * packageName    : io.miraclecat.fbs.auth.controller
 * fileName       : ErrorPageController
 * author         : MiracleCat 
 * date           : 2024-05-02
 * description    :
 * 2025-08-26        MiracleCat       HTML 에러 페이지만 처리
 */
@Slf4j
@Controller
public class ErrorPageController implements ErrorController {

    /**
     * HTML 요청 에러 페이지
     */
    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleHtmlError(HttpServletRequest request,
                                        Model model) {
        Integer status = (Integer) model.asMap().get("status");
        String uri = (String) model.asMap().get("uri");
        Integer statusCode = status != null ? status :
            (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = uri != null ? uri :
        (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        log.warn("HTML Error occurred - Status: {}, URI: {}", statusCode, requestUri);

        ModelAndView mav = new ModelAndView();

        if (statusCode != null) {
            if (statusCode == 404) {
                mav.setViewName("/error/404");
            } else if (statusCode == 403) {
                mav.setViewName("/error/403");
            } else if (statusCode >= 500) {
                mav.setViewName("/error/error");
            } else {
                mav.setViewName("/error/error");
            }
        } else {
            mav.setViewName("/error/error");
        }

        mav.addObject("status", statusCode);
        mav.addObject("requestUri", requestUri);
        
        return mav;
    }

    /**
     * 접근 거부 페이지
     */
    @RequestMapping(value = "/denied", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleAccessDenied() {
        log.warn("Access denied occurred");
        return new ModelAndView("/error/403");
    }

}
