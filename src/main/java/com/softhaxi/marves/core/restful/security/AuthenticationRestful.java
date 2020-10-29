package com.softhaxi.marves.core.restful.security;

import java.time.ZonedDateTime;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Employee;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.model.request.LoginRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.service.logging.LoggerService;
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

    @Autowired
    private LoggerService loggerService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = null;
        String description = "login.mobile";
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserid().trim(), request.getPassword().trim()));

            user = userService.findByUsername(request.getUserid().trim()).orElse(null);
            if (user == null) {
                user = new User()
                    .username(request.getUserid().trim().toUpperCase());   
                // user.setPassword(request.getPassword().trim());
                user.setIsLDAPUser(true);

                Map<?, ?> userLdap = (Map<?, ?>) userService.retrieveUserLdapDetail(request.getUserid().trim().toLowerCase());
                Employee employee = null;
                Profile profile = null;
                if(userLdap != null) {
                    logger.debug("[login] User ldap..." + userLdap.toString());
                    user.setEmail(userLdap.get("email").toString());
                    profile = new Profile().fullName(userLdap.get("fullName").toString())
                        .primaryEmail(userLdap.get("email").toString());;
                    employee = new Employee().employeeNo(userLdap.get("employeeNo").toString());
                
                }
                user.setProfile(profile);
                user.setEmployee(employee);
                user = userService.saveMobileUser(user);
                description = "first.time.login.mobile";
            }

            loggerService.saveAsyncActivityLog(
                new ActivityLog().user(user)
                    .actionTime(ZonedDateTime.now())
                    .actionName("log.in")
                    .description(description)
                    .uri("/user")
                    .referenceId(user.getId().toString())
                    .deepLink(String.format("core://marves.dev/user?id=%s", user.getId().toString()))
            );

            return new ResponseEntity<>(
                new GeneralResponse(
                HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(), 
                Map.of("type", "Bearer", "accessToken", accessTokenUtil.generateToken(user.getId().toString())))
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
