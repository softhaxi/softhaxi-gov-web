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
            
            UserRole userRole = new UserRole(sUser, sadmin);
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
            
            UserRole userRole = new UserRole(aUser, admin);
            userRoleRepository.save(userRole);
        }

        Office hOffice = officeRepository.findHeadOffice().orElse(new Office());
        if(hOffice.getId() == null) {
            hOffice.setCode("HO0001");
            hOffice.setName("Kementerian Koordiantor Kemaritiman dan Investasi");
            hOffice.setType("HO");
            hOffice.setLatitude(-6.184843);
            hOffice.setLongitude(106.822793);
            officeRepository.save(hOffice);
        }

        SystemParameter radiusLimit = sysParamRepository.findByCode("WFO_RADIUS_LIMIT").orElse(new SystemParameter());
        if(radiusLimit.getId() == null) {
            radiusLimit.setCode("WFO_RADIUS_LIMIT");
            radiusLimit.setName("WFO Radius Limit in metre (m)");
            radiusLimit.setValue("5");
            radiusLimit.setDecription("Radius limit to indicate that clock in/out is from office");
            radiusLimit.setIsEditable(true);
            radiusLimit.setIsSystem(false);
            sysParamRepository.save(radiusLimit);
        }

        SystemParameter houseKeep = sysParamRepository.findByCode("EXCLUDE_HOUSE_KEEP").orElse(new SystemParameter());
        if(houseKeep.getId() == null) {
            houseKeep.setCode("HOUSE_KEEP_DAYS");
            houseKeep.setName("House keeping days");
            houseKeep.setValue("180");
            houseKeep.setDecription("Keep record in master table only for specific days otherwise save to archive table");
            houseKeep.setIsEditable(true);
            houseKeep.setIsSystem(false);
            sysParamRepository.save(houseKeep);
        }

        SystemParameter excludeHouseKeep = sysParamRepository.findByCode("EXCLUDE_HOUSE_KEEP").orElse(new SystemParameter());
        if(excludeHouseKeep.getId() == null) {
            excludeHouseKeep.setCode("EXCLUDE_HOUSE_KEEP");
            excludeHouseKeep.setName("Exclude log tables");
            excludeHouseKeep.setValue("N/A");
            excludeHouseKeep.setDecription("List of exclude log tables for house keeping process");
            excludeHouseKeep.setIsEditable(true);
            excludeHouseKeep.setIsSystem(false);
            sysParamRepository.save(excludeHouseKeep);
        }

        SystemParameter marvesHRAPI = sysParamRepository.findByCode("MARVESHR_API_URL").orElse(new SystemParameter());
        if(marvesHRAPI.getId() == null) {
            marvesHRAPI.setCode("MARVESHR_API_URL");
            marvesHRAPI.setName("Marves HR Restful Service");
            marvesHRAPI.setValue("https://marveshr.maritim.go.id/webservice");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            marvesHRAPI.setIsEditable(true);
            marvesHRAPI.setIsSystem(false);
            sysParamRepository.save(marvesHRAPI);
        }

        SystemParameter marvesLetterAPI = sysParamRepository.findByCode("MARVESLETTER_API_URL").orElse(new SystemParameter());
        if(marvesLetterAPI.getId() == null) {
            marvesLetterAPI.setCode("MARVESLETTER_API_URL");
            marvesLetterAPI.setName("Marves Persuratan Restful Service");
            marvesLetterAPI.setValue("https://persuratan.maritim.go.id/webservice");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            marvesLetterAPI.setIsEditable(true);
            marvesLetterAPI.setIsSystem(false);
            sysParamRepository.save(marvesLetterAPI);
        }

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
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("DISPENSATION_MAX_DAYS");
            covidTrackerAPI.setName("Maximum Input dispensation in days");
            covidTrackerAPI.setValue("14");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(maxDispensationDay);
        }



        SystemParameter maxClockInDaily = sysParamRepository.findByCode("MAX_CLOCK_IN_DAILY").orElse(new SystemParameter());
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("MAX_CLOCK_IN_DAILY");
            covidTrackerAPI.setName("Maximum Clock In Daily");
            covidTrackerAPI.setValue("07:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(maxClockInDaily);
        }

        SystemParameter maxClockOutDaily = sysParamRepository.findByCode("MAX_CLOCK_OUT_DAILY").orElse(new SystemParameter());
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("MAX_CLOCK_OUT_DAILY");
            covidTrackerAPI.setName("Maximum Clock Out Daily");
            covidTrackerAPI.setValue("16:00:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(maxClockOutDaily);
        }

        SystemParameter maxClockInFriday = sysParamRepository.findByCode("MAX_CLOCK_IN_FRIDAY").orElse(new SystemParameter());
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("MAX_CLOCK_IN_FRIDAY");
            covidTrackerAPI.setName("Max Clock In Friday");
            covidTrackerAPI.setValue("07:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(maxClockInFriday);
        }

        SystemParameter maxClockOutFriday = sysParamRepository.findByCode("MAX_CLOCK_OUT_FRIDAY").orElse(new SystemParameter());
        if(covidTrackerAPI.getId() == null) {
            covidTrackerAPI.setCode("MAX_CLOCK_OUT_FRIDAY");
            covidTrackerAPI.setName("Maximum Clock Out Friday");
            covidTrackerAPI.setValue("16:30:00");
            //marvesHRAPI.setDecription("Radius limit to indicate that clock in/out is from office");
            covidTrackerAPI.setIsEditable(true);
            covidTrackerAPI.setIsSystem(false);
            sysParamRepository.save(maxClockOutFriday);
        }


        logger.info("[run] Finish at " + ZonedDateTime.now());
    }
    
}
