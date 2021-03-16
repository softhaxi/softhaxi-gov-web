package com.softhaxi.marves.core.restful.security;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.access.UserRole;
import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.domain.logging.LocationLog;
import com.softhaxi.marves.core.domain.logging.Session;
import com.softhaxi.marves.core.domain.request.LoginRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.access.UserRoleRepository;
import com.softhaxi.marves.core.repository.account.ProfileRepository;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.service.employee.EmployeeVitaeService;
import com.softhaxi.marves.core.service.logging.LoggerService;
import com.softhaxi.marves.core.service.message.ChatService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private ProfileRepository profileRepo;

    @Autowired
    private EmployeeVitaeService employeeVitaeService;

    @Autowired
    private LoggerService loggerService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private UserRoleRepository userRoleRepo;

    @Autowired
    private SessionRepository sessionRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servlet) {
        logger.debug("[login] Request...." + request.toString());
        User user = null;
        String description = "login.mobile";
        String ipAddress = servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For") : servlet.getRemoteAddr();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserid().trim(), request.getPassword().trim()));

            user = userService.findByUsername(request.getUserid().trim()).orElse(null);
            if(request.getUserid().trim().contains("@maritim.go.id") && user == null) {
                user = userService.findByUsername(request.getUserid().trim().substring(0, request.getUserid().indexOf("@"))).orElse(null);
            }
            Profile profile = null;
            Map<?, ?> userLdap = null;
            Map<?, ?> profileData = null;
            boolean saveChat = false;
            if (user == null) {
                saveChat = true;
                userLdap = (Map<?, ?>) userService.retrieveUserLdapDetail(request.getUserid().trim().toLowerCase());
                // Employee employee = null;
                user = new User();
                if (userLdap != null) {
                    logger.debug("[login] User ldap..." + userLdap.toString());
                    profileData = (Map<?, ?>) employeeVitaeService
                            .getPersonalInfo(userLdap.get("email").toString().toLowerCase().trim());
                    user.setUsername(userLdap.get("username").toString().trim().toUpperCase());
                    user.setIsLDAPUser(true);
                    user.setEmail(userLdap.get("email").toString());
                    if (profileData != null) {
                        profile = new Profile().fullName(profileData.get("name").toString())
                                .primaryEmail(user.getEmail());
                    } else {
                        profile = new Profile().fullName(userLdap.get("fullName").toString())
                                .primaryEmail(user.getEmail());
                    }
                }
                user.setProfile(profile);
                user.setStatus("ACTIVE");
                user.setOneSignalId(request.getOneSignalId());
                user = userService.saveMobileUser(user);
                description = "first.time.login.mobile";
            } else {
                profile = profileRepo.findByUser(user).orElse(null);
                if (profile == null) {
                    saveChat = true;
                    userLdap = (Map<?, ?>) userService.retrieveUserLdapDetail(user.getEmail().trim().toLowerCase());
                    profileData = (Map<?, ?>) employeeVitaeService
                            .getPersonalInfo(user.getEmail().toLowerCase().trim());
                    if (profileData != null) {
                        profile = new Profile().fullName(profileData.get("name").toString())
                                .primaryEmail(userLdap.get("email").toString());
                    } else {
                        profile = new Profile().fullName(userLdap.get("fullName").toString())
                                .primaryEmail(userLdap.get("email").toString());
                    }
                    user.setStatus("ACTIVE");
                    user.setOneSignalId(request.getOneSignalId());
                    profile.setUser(user);
                    profileRepo.save(profile);
                    Role role = roleRepo.findByName("MOBILE").orElse(null);
                    if (role != null) {
                        userRoleRepo.save(new UserRole(user, role, false));
                    }
                    description = "first.time.login.mobile";
                } else {
                    user.setOneSignalId(request.getOneSignalId());
                    saveChat = false;
                }
            }

            if (!user.getUsername().equalsIgnoreCase("MCORE.ADMIN")) {
                if(request.getLatitude() != null && request.getLongitude() != null)
                    loggerService.saveAsyncLocationLog(new LocationLog().user(user).dateTime(ZonedDateTime.now())
                    .isMockLocation(request.isMockLocation())
                    .latitude(Double.parseDouble(request.getLatitude()))
                    .longitude(Double.parseDouble(request.getLongitude())));
                loggerService.saveAsyncActivityLog(new ActivityLog().user(user).actionTime(ZonedDateTime.now())
                        .actionName("log.in").description(description).uri("/user").deepLink("core://marves.dev/user")
                        .referenceId(user.getId().toString()).ipAddress(ipAddress));

                if (saveChat)
                    chatService.sendWelcomeMessage(user);
            }

            List<Session> sessions = (List<Session>) sessionRepo.findAllValidByUser(user);
            if(sessions != null && !sessions.isEmpty()) {
                sessions.forEach((session) -> {
                    session.setStatus("INVALID");
                });
            }
            sessionRepo.saveAll(sessions);
            Session session = new Session()
                .user(user)
                .type("Bearer")
                .accessToken(accessTokenUtil.generateToken(user.getId().toString()))
                .status("VALID")
                .oneSignalId(request.getOneSignalId());
            sessionRepo.save(session);

            return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
                    Map.of("type", session.getType(),
                        "accessToken", session.getAccessToken())),
                    HttpStatus.OK);
        } catch (DisabledException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid userid and password"), HttpStatus.BAD_REQUEST);
        } catch (BadCredentialsException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid userid and password"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest servlet) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        String ipAddress = servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For") : servlet.getRemoteAddr();
        
        try {
            List<Session> sessions = (List<Session>) sessionRepo.findAllValidByUser(user);
            if(sessions != null && !sessions.isEmpty()) {
                sessions.forEach((session) -> {
                    session.setStatus("INVALID");
                });
            }
            sessionRepo.saveAll(sessions);

            loggerService.saveAsyncActivityLog(new ActivityLog().user(user).actionTime(ZonedDateTime.now())
                        .actionName("log.out").description("logout.from.mobile").uri("/user").deepLink("core://marves.dev/user")
                        .referenceId(user.getId().toString()).ipAddress(ipAddress));

            return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(),
                    "logout.successful"),
                    HttpStatus.OK);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), "unable.to.logout"), HttpStatus.BAD_REQUEST);
        }
    }
}
