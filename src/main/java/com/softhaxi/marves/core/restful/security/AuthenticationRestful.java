package com.softhaxi.marves.core.restful.security;

import com.softhaxi.marves.core.model.request.LoginRequest;
import com.softhaxi.marves.core.model.response.Response;
import com.softhaxi.marves.core.model.response.common.LoginResponse;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserid(), request.getPassword()));

            // final User userDetails = userService.loadUserByUsername(authenticationRequest.getUsername());
            // final String token = jwtTokenUtil.generateToken(userDetails);

            
            LoginResponse loginResponse = new LoginResponse("bearer", accessTokenUtil.generateToken(request.getUserid()), null);
            Response<LoginResponse> response = new Response<>(HttpStatus.OK.value(), HttpStatus.OK.toString(), loginResponse);

            return ResponseEntity.ok(response);
        } catch(DisabledException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>("Invalid userid and password", HttpStatus.BAD_REQUEST);
        } catch(BadCredentialsException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>("Invalid userid and password", HttpStatus.BAD_REQUEST);
        }
    }
}
