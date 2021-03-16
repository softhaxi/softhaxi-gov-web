package com.softhaxi.marves.core.scheduler;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Employee;
import com.softhaxi.marves.core.repository.account.ProfileRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.EmployeeRepository;
import com.softhaxi.marves.core.service.employee.EmployeeVitaeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDetail {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDetail.class);

    @Autowired
    private EmployeeVitaeService vitaeService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProfileRepository profileRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @Scheduled(cron = "${cron.employee.batch}")
    public void syncDetail() {
        logger.debug("[syncDetail] Start at " + LocalDateTime.now());
        try {
            Collection<User> users = userRepo.findAllActiveMobileUser();
            Collection<Employee> employees = new LinkedList<>();
            Collection<Profile> profiles = new LinkedList<>();
            users.stream().forEach((user) -> {
                Map<?, ?> profileData = vitaeService.getPersonalInfo(user.getEmail());
                Employee employee = null;
                Profile profile = null;
                if (user.getEmployee() != null) {
                    employee = user.getEmployee();
                } else {
                    employee = new Employee().user(new User().id(user.getId()));
                }
                if (user.getProfile() != null) {
                    profile = user.getProfile();
                }
                if (profileData != null) {
                    employee.setEmployeeNo(
                            profileData.get("employeeNumber") != null ? profileData.get("employeeNumber").toString()
                                    : employee.getEmployeeNo() != null ? employee.getEmployeeNo() : "");
                    employee.setPictureUrl(
                            profileData.get("thumbnail") != null ? profileData.get("thumbnail").toString() : employee.getPictureUrl());
                    employee.setDivisionName(
                            profileData.get("division") != null ? profileData.get("division").toString() : employee.getDivisionName());
                    employees.add(employee);
                    
                    if(profile != null) {
                        profile.setFullName(profileData.get("name") != null ? profileData.get("name").toString() : profile.getFirstName() != null ? profile.getFullName() : "");
                        // profile.setPrimaryEmail(profileData.get("email") != null ? profileData.get("email").toString() : profile.getPrimaryEmail());
                        // user.setEmail(profileData.get("email") != null ? profileData.get("email").toString() : user.getEmail());
                        profiles.add(profile);
                    }
                }
            });
            employeeRepo.saveAll(employees);
            profileRepo.saveAll(profiles);
            // userRepo.saveAll(users);
        } catch (Exception ex) {
            logger.error("[syncDetail] Error " + ex.getMessage(), ex);
        } finally {
            logger.debug("[syncDetail] Finish at " + LocalDateTime.now());
        }
    }
}
