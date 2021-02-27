package com.softhaxi.marves.core.web;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Component
public class RestfulAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(RestfulAuthenticationEntryPoint.class);
    

    /**
     *
     */
    private static final long serialVersionUID = -6600248561581866905L;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if(request.getRequestURI().startsWith("/api")) {
            logger.error("Responding with unauthorized error. Message - {}", authException.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
        }
    }
    
}
