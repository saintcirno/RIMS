package com.rims.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, Model model) {
        log.error("[RIMS Unhandled Exception] Error at URI: {} - Exception: ", request.getRequestURI(), ex);
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("errorMessage", ex.getMessage() != null && !ex.getMessage().isBlank() 
                ? ex.getMessage() 
                : "An unexpected system error occurred while processing your request.");
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error";
    }
}
