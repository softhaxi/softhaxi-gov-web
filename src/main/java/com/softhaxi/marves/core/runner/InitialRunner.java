package com.softhaxi.marves.core.runner;

import java.time.ZonedDateTime;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.access.UserRole;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.master.Office;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.access.UserRoleRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.master.OfficeRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Raja Sihonbing
 * @since 1
 */
@Component
public class InitialRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitialRunner.class);

    @Autowired
    private RoleRepository roleRepository; 

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private SystemParameterRepository sysParamRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("[run] Start at " + ZonedDateTime.now());
        var count = roleRepository.count();
        if(count == 0) { 
            Role sadmin = new Role();
            sadmin.setName("SADMIN");
            sadmin.setDescription("Super Administrator");
            sadmin.setIsSystem(true);
            roleRepository.save(sadmin);

            Role admin = new Role();
            admin.setName("ADMIN");
            admin.setDescription("Administrator");
            admin.setIsSystem(false);
            roleRepository.save(admin);

            Role user = new Role();
            user.setName("MOBILE");
            user.setDescription("Mobile User");
            user.setIsSystem(false);
            roleRepository.save(user);
        }
        Role operator = roleRepository.findByName("OPERATOR").orElse(new Role());
        if(operator.getId() == null) {
            operator.setName("OPERATOR");
            operator.setDescription("Operator");
            operator.setIsSystem(false);
            roleRepository.save(operator);
        }

        Role sadmin = roleRepository.findByName("SADMIN").orElse(null);
        User sUser = userRepository.findByUsername("MCORE.SADMIN").orElse(null);
        if(sUser == null && sadmin != null) {
            sUser = new User();
            sUser.setUsername("MCORE.SADMIN");
            sUser.setEmail("mcore.sadmin@maritim.go.id");
            sUser.setPassword("password");
            sUser.setIsLDAPUser(false);
            //sUser.setRoles(new HashSet<>(Arrays.asList(new UserRole(sUser, sadmin))));
            userRepository.save(sUser);
            
            UserRole userRole = new UserRole(sUser, sadmin, false);
            userRoleRepository.save(userRole);
        }

        User aOpr = userRepository.findByUsername("MCORE.OPERATOR").orElse(null);
        if(aOpr == null && operator != null) {
            aOpr = new User();
            aOpr.setUsername("MCORE.OPERATOR");
            aOpr.setEmail("mcore.operator@maritim.go.id");
            aOpr.setPassword("password");
            aOpr.setIsLDAPUser(false);
            userRepository.save(aOpr);
            
            UserRole userRole = new UserRole(aOpr, operator, false);
            userRoleRepository.save(userRole);
        }

        Role admin = roleRepository.findByName("ADMIN").orElse(null);
        User aUser = userRepository.findByUsername("MCORE.ADMIN").orElse(null);
        if(aUser == null && sadmin != null) {
            aUser = new User();
            aUser.setUsername("MCORE.ADMIN");
            aUser.setEmail("mcore.admin@maritim.go.id");
            // aUser.setPassword("password");
            aUser.setIsLDAPUser(true);
            //sUser.setRoles(new HashSet<>(Arrays.asList(new UserRole(sUser, sadmin))));
            userRepository.save(aUser);
            
            UserRole userRole = new UserRole(aUser, admin, false);
            userRoleRepository.save(userRole);
        }

        Office hOffice = officeRepository.findHeadOffice().orElse(new Office());
        if(hOffice.getId() == null) {
            hOffice.setCode("HO0001");
            hOffice.setName("Kementerian Koordiantor Kemaritiman dan Investasi");
            hOffice.setType("HO");
            //hOffice.setLatitude(-6.184843);
            //hOffice.setLongitude(106.822793);
            officeRepository.save(hOffice);
        }

        SystemParameter radiusLimit = sysParamRepository.findByCode("WFO_RADIUS_LIMIT").orElse(new SystemParameter());
        if(radiusLimit.getId() == null) {
            radiusLimit.setCode("WFO_RADIUS_LIMIT");
            radiusLimit.setName("WFO Radius Limit in metre (m)");
            radiusLimit.setValue("100");
            radiusLimit.setDecription("Radius limit to indicate that clock in/out is from office");
            radiusLimit.setIsEditable(true);
            radiusLimit.setIsSystem(false);
            sysParamRepository.save(radiusLimit);
        }

        SystemParameter houseKeep = sysParamRepository.findByCode("HOUSE_KEEP_DAYS").orElse(new SystemParameter());
        if(houseKeep.getId() == null) {
            houseKeep.setCode("HOUSE_KEEP_DAYS");
            houseKeep.setName("House keeping days");
            houseKeep.setValue("180");
            houseKeep.setDecription("Keep record in master table only for specific days otherwise save to archive table");
            houseKeep.setIsEditable(true);
            houseKeep.setIsSystem(false);
            sysParamRepository.save(houseKeep);
        }

        SystemParameter excludeHouseKeep = sysParamRepository.findByCode("HOUSE_KEEP_EXCLUDES").orElse(new SystemParameter());
        if(excludeHouseKeep.getId() == null) {
            excludeHouseKeep.setCode("HOUSE_KEEP_EXCLUDES");
            excludeHouseKeep.setName("Exclude log tables");
            excludeHouseKeep.setValue("N/A");
            excludeHouseKeep.setDecription("List of exclude log tables for house keeping process");
            excludeHouseKeep.setIsEditable(true);
            excludeHouseKeep.setIsSystem(false);
            sysParamRepository.save(excludeHouseKeep);
        }

        // SystemParameter marvesHRAPI = sysParamRepository.findByCode("MARVESHR_API_URL").orElse(new SystemParameter());
        // if(marvesHRAPI.getId() == null) {
        //     marvesHRAPI.setCode("MARVESHR_API_URL");
        //     marvesHRAPI.setName("Marves HR Restful Service");
        //     marvesHRAPI.setValue("https://marveshr.maritim.go.id/webservice");
        //     //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
        //     marvesHRAPI.setIsEditable(true);
        //     marvesHRAPI.setIsSystem(false);
        //     sysParamRepository.save(marvesHRAPI);
        // }

        // SystemParameter marvesLetterAPI = sysParamRepository.findByCode("MARVESLETTER_API_URL").orElse(new SystemParameter());
        // if(marvesLetterAPI.getId() == null) {
        //     marvesLetterAPI.setCode("MARVESLETTER_API_URL");
        //     marvesLetterAPI.setName("Marves Persuratan Restful Service");
        //     marvesLetterAPI.setValue("https://persuratan.maritim.go.id/webservice");
        //     //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
        //     marvesLetterAPI.setIsEditable(true);
        //     marvesLetterAPI.setIsSystem(false);
        //     sysParamRepository.save(marvesLetterAPI);
        // }

        SystemParameter covidTrackerAPI = sysParamRepository.findByCode("COVIDTRACKER_API_URL").orElse(new SystemParameter());
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("COVIDTRACKER_API_URL");
            covidTrackerAPI.setName("Covid Tracker Restful Service");
            covidTrackerAPI.setValue("https://covidtracker.maritim.go.id/api");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(covidTrackerAPI);
        }

        SystemParameter maxDispensationDay = sysParamRepository.findByCode("DISPENSATION_MAX_DAYS").orElse(new SystemParameter());
        if(maxDispensationDay.getId() == null) {
            maxDispensationDay.setCode("DISPENSATION_MAX_DAYS");
            maxDispensationDay.setName("Maximum Input dispensation in days");
            maxDispensationDay.setValue("14");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            maxDispensationDay.setIsEditable(true);
            maxDispensationDay.setIsSystem(false);
            sysParamRepository.save(maxDispensationDay);
        }



        SystemParameter maxClockInDaily = sysParamRepository.findByCode("CLOCKIN_MAX").orElse(new SystemParameter());
        if(maxClockInDaily.getId() == null) {
            maxClockInDaily.setCode("CLOCKIN_MAX");
            maxClockInDaily.setName("Maximum Clock In Daily");
            maxClockInDaily.setValue("07:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            maxClockInDaily.setIsEditable(true);
            maxClockInDaily.setIsSystem(false);
            sysParamRepository.save(maxClockInDaily);
        }

        SystemParameter maxClockOutDaily = sysParamRepository.findByCode("CLOCKOUT_MAX").orElse(new SystemParameter());
        if(maxClockOutDaily.getId() == null) {
            maxClockOutDaily.setCode("CLOCKOUT_MAX");
            maxClockOutDaily.setName("Maximum Clock Out Daily");
            maxClockOutDaily.setValue("16:00:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            maxClockOutDaily.setIsEditable(true);
            maxClockOutDaily.setIsSystem(false);
            sysParamRepository.save(maxClockOutDaily);
        }

        SystemParameter maxClockInFriday = sysParamRepository.findByCode("CLOCKIN_MAX_FRIDAY").orElse(new SystemParameter());
        if(maxClockInFriday.getId() == null) {
            maxClockInFriday.setCode("CLOCKIN_MAX_FRIDAY");
            maxClockInFriday.setName("Max Clock In Friday");
            maxClockInFriday.setValue("07:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            maxClockInFriday.setIsEditable(true);
            maxClockInFriday.setIsSystem(false);
            sysParamRepository.save(maxClockInFriday);
        }

        SystemParameter maxClockOutFriday = sysParamRepository.findByCode("CLOCKOUT_MAX_FRIDAY").orElse(new SystemParameter());
        if(maxClockOutFriday.getId() == null) {
            maxClockOutFriday.setCode("CLOCKOUT_MAX_FRIDAY");
            maxClockOutFriday.setName("Maximum Clock Out Friday");
            maxClockOutFriday.setValue("16:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            maxClockOutFriday.setIsEditable(true);
            maxClockOutFriday.setIsSystem(false);
            sysParamRepository.save(maxClockOutFriday);
        }

        SystemParameter paginationPageSize = sysParamRepository.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter());
        if(paginationPageSize.getId() == null) {
            paginationPageSize.setCode("PAGINATION_PAGE_SIZE");
            paginationPageSize.setName("Number of record in page for table and size");
            paginationPageSize.setValue("10");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            paginationPageSize.setIsEditable(true);
            paginationPageSize.setIsSystem(false);
            sysParamRepository.save(paginationPageSize);
        }

        SystemParameter clockInReminder = sysParamRepository.findByCode("CLOCKIN_REMINDER_ENABLED").orElse(new SystemParameter());
        if(clockInReminder.getId() == null) {
            clockInReminder.setCode("CLOCKIN_REMINDER_ENABLED");
            clockInReminder.setName("To enable daily clock in reminder notification");
            clockInReminder.setValue("Y");
            clockInReminder.setIsEditable(true);
            clockInReminder.setIsSystem(false);
            sysParamRepository.save(clockInReminder);
        }

        clockInReminder = sysParamRepository.findByCode("CLOCKIN_REMINDER_TIME").orElse(new SystemParameter());
        if(clockInReminder.getId() == null) {
            clockInReminder.setCode("CLOCKIN_REMINDER_TIME");
            clockInReminder.setName("Time to send clock in reminder notification (HH:mm)");
            clockInReminder.setValue("07:30");
            clockInReminder.setIsEditable(true);
            clockInReminder.setIsSystem(false);
            sysParamRepository.save(clockInReminder);
        }

        SystemParameter clockOutReminder = sysParamRepository.findByCode("CLOCKOUT_REMINDER_ENABLED").orElse(new SystemParameter());
        if(clockOutReminder.getId() == null) {
            clockOutReminder.setCode("CLOCKOUT_REMINDER_ENABLED");
            clockOutReminder.setName("To enable daily clock out reminder notification");
            clockOutReminder.setValue("N");
            clockOutReminder.setIsEditable(true);
            clockOutReminder.setIsSystem(false);
            sysParamRepository.save(clockOutReminder);
        }

        clockOutReminder = sysParamRepository.findByCode("CLOCKOUT_REMINDER_TIME").orElse(new SystemParameter());
        if(clockOutReminder.getId() == null) {
            clockOutReminder.setCode("CLOCKOUT_REMINDER_TIME");
            clockOutReminder.setName("Time to send clock in reminder notification (HH:mm)");
            clockOutReminder.setValue("16:00");
            clockOutReminder.setIsEditable(true);
            clockOutReminder.setIsSystem(false);
            sysParamRepository.save(clockOutReminder);
        }

        SystemParameter agendaReminder = sysParamRepository.findByCode("AGENDA_REMINDER_BEFORE").orElse(new SystemParameter());
        if(agendaReminder.getId() == null) {
            agendaReminder.setCode("AGENDA_REMINDER_BEFORE");
            agendaReminder.setName("To send reminder before agenda time (in minutes)");
            agendaReminder.setValue("60");
            agendaReminder.setIsEditable(true);
            agendaReminder.setIsSystem(false);
            sysParamRepository.save(agendaReminder);
        }

        SystemParameter bufferTime = sysParamRepository.findByCode("CLOCKIN_BUFFER_TIME").orElse(new SystemParameter());
        if(bufferTime.getId() == null) {
            bufferTime.setCode("CLOCKIN_BUFFER_TIME");
            bufferTime.setName("Buffer time to absence from max clock in (in minutes)");
            bufferTime.setValue("60");
            bufferTime.setIsEditable(true);
            bufferTime.setIsSystem(false);
            sysParamRepository.save(bufferTime);
        }

        logger.info("[run] Finish at " + ZonedDateTime.now());
    }
    
}
