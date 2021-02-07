package com.softhaxi.marves.core.restful.security;

import java.time.ZonedDateTime;
import java.util.Map;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.access.UserRole;
import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.model.request.LoginRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.access.UserRoleRepository;
import com.softhaxi.marves.core.repository.account.ProfileRepository;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = null;
        String description = "login.mobile";
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserid().trim(), request.getPassword().trim()));

            user = userService.findByUsername(request.getUserid().trim()).orElse(null);
            Profile profile = null;
            Map<?, ?> userLdap = null;
            Map<?, ?> profileData = null;
            boolean saveChat = false;
            if (user == null) {
                saveChat = true;
                userLdap = (Map<?, ?>) userService.retrieveUserLdapDetail(request.getUserid().trim().toLowerCase());
                //Employee employee = null;
                user = new User();
                if(userLdap != null) {
                    logger.debug("[login] User ldap..." + userLdap.toString());
                    profileData = (Map<?, ?>) employeeVitaeService.getPersonalInfo(userLdap.get("email").toString().toLowerCase().trim());
                    user.setUsername(userLdap.get("username").toString().trim().toUpperCase());  
                    user.setIsLDAPUser(true);
                    if(profileData != null) {
                        user.setEmail(profileData.get("email").toString());
                        profile = new Profile().fullName(profileData.get("name").toString())
                            .primaryEmail(profileData.get("email").toString()); 
                    } else {
                        user.setEmail(userLdap.get("email").toString());
                        profile = new Profile().fullName(userLdap.get("fullName").toString())
                            .primaryEmail(userLdap.get("email").toString());
                    }
                }
                user.setProfile(profile);
                user.setStatus("ACTIVE");
                user.setOneSignalId(request.getOneSignalId());
                user = userService.saveMobileUser(user);
                description = "first.time.login.mobile";
            } else {
                profile = profileRepo.findByUser(user).orElse(null);
                if(profile == null) {
                    saveChat = true;
                    userLdap = (Map<?, ?>) userService.retrieveUserLdapDetail(user.getEmail().trim().toLowerCase());
                    profileData = (Map<?, ?>) employeeVitaeService.getPersonalInfo(user.getEmail().toLowerCase().trim());
                    if(profileData != null) {
                        profile = new Profile().fullName(profileData.get("name").toString())
                            .primaryEmail(profileData.get("email").toString()); 
                    } else {
                        profile = new Profile().fullName(userLdap.get("fullName").toString())
                            .primaryEmail(userLdap.get("email").toString());
                    }
                    user.setStatus("ACTIVE");
                    user.setOneSignalId(request.getOneSignalId());
                    profile.setUser(user);
                    profileRepo.save(profile);
                    Role role = roleRepo.findByName("MOBILE").orElse(null);
                    if(role != null) {
                        userRoleRepo.save(new UserRole(user, role));
                    }
                    description = "first.time.login.mobile";
                } else {
                    user.setOneSignalId(request.getOneSignalId()); 
                    saveChat = false;
                }
            }

            if(!user.getUsername().equalsIgnoreCase("MCORE.ADMIN")) {
                loggerService.saveAsyncActivityLog(
                    new ActivityLog().user(user)
                        .actionTime(ZonedDateTime.now())
                        .actionName("log.in")
                        .description(description)
                        .uri("/user")
                        .deepLink("core://marves.dev/user")
                        .referenceId(user.getId().toString())
                );

                if(saveChat)
                    chatService.sendWelcomeMessage(user);
            }

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
