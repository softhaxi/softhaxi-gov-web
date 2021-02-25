package com.softhaxi.marves.core.restful.support;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.LocationLog;
import com.softhaxi.marves.core.model.request.LocationRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.logging.LoggerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/location")
public class LocationResful {

    private static final Logger logger = LoggerFactory.getLogger(LocationResful.class);

    @Autowired
    private LoggerService loggerService;
    
    @PostMapping()
    public ResponseEntity<?> post(@RequestBody LocationRequest request,
        HttpServletRequest servlet) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        logger.debug("[index] Here ......");

        String ipAddress = servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For") : servlet.getRemoteAddr();
        
        try {
            LocationLog location = new LocationLog()
                .user(user)
                .dateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isMockLocation(request.isMockLocation());
            // locationRepo.save(location);
            loggerService.saveAsyncLocationLog(location);

            return new ResponseEntity<>(
                new GeneralResponse(
                    HttpStatus.CREATED.value(), 
                    HttpStatus.CREATED.getReasonPhrase(), 
                    location
                ), 
                HttpStatus.CREATED);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    ex.getMessage()
                ), 
                HttpStatus.BAD_REQUEST);
        } finally {

        }
        
    }
}
