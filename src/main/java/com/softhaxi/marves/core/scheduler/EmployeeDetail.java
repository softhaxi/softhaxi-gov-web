package com.softhaxi.marves.core.scheduler;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Employee;
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
    private EmployeeRepository employeeRepo;

    @Scheduled(cron = "${cron.employeeDivision.batch}")
    public void syncEmployeeDivision() {
        logger.debug("[syncEmployeeDivision] Start at " + LocalDateTime.now());
        try {
            Collection<User> users = userRepo.findAllNonAdminUsers();
            
            users.stream().forEach((user) -> {
                Map<?, ?> profileData = vitaeService.getPersonalInfo(user.getEmail());
                Employee employee = null;
                if(user.getEmployee() != null) {
                    employee = user.getEmployee();
                } else {
                    employee = new Employee().user(user);
                }
                if(profileData != null) {
                    employee.setEmployeeNo(profileData.get("employeeNumber") != null ? profileData.get("employeeNumber").toString() : "");
                    employee.setDivisionName(profileData.get("division") != null ? profileData.get("division").toString() : null);
                    employeeRepo.save(employee);
                }
            });
        } catch (Exception ex) {

        } finally {
            logger.debug("[syncEmployeeDivision] Finish at " + LocalDateTime.now());
        }
    }
}
