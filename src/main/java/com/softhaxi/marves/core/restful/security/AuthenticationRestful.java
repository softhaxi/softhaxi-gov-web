package com.softhaxi.marves.core.restful.security;

import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.request.LoginRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.util.AccessTokenUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationRestful {

    private static Logger logger = LoggerFactory.getLogger(AuthenticationRestful.class);

    @Autowired
    @Qualifier(value = "restAuthenticationManager")
    private AuthenticationManager authenticationManager;

    @Autowired
    private AccessTokenUtil accessTokenUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserid().trim(), request.getPassword().trim()));

            User user = userService.findByUsername(request.getUserid().trim()).orElse(null);
            if (user == null) {
                user = new User();
                user.setUsername(request.getUserid().trim());   
                user.setPassword(request.getPassword().trim());
                user.setIsLDAPUser(true);
                user = userService.saveMobileUser(user);
            }

            return new ResponseEntity<>(
                new GeneralResponse(
                HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(), 
                Map.of("type", "bearer", "accessToken", accessTokenUtil.generateToken(user.getId().toString())))
                , HttpStatus.OK);
        } catch(DisabledException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Invalid userid and password"
                ), HttpStatus.BAD_REQUEST);
        } catch(BadCredentialsException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Invalid userid and password"
                ), HttpStatus.BAD_REQUEST);
        }
    }
}
