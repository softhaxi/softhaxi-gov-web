package com.softhaxi.marves.core.runner;

import java.util.Date;

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
        logger.info("[InitialRunner][run] Start at " + new Date(System.currentTimeMillis()));
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
        User sUser = userRepository.findByUsername("SADMIN").orElse(null);
        if(sUser == null && sadmin != null) {
            sUser = new User();
            sUser.setUsername("SADMIN");
            sUser.setEmail("marvescore@gmail.com");
            sUser.setPassword("MarvesCore123$.");
            sUser.setIsLDAPUser(false);
            //sUser.setRoles(new HashSet<>(Arrays.asList(new UserRole(sUser, sadmin))));
            userRepository.save(sUser);
            
            UserRole userRole = new UserRole(sUser, sadmin);
            userRoleRepository.save(userRole);
        }

        Role admin = roleRepository.findByName("ADMIN").orElse(null);
        User aUser = userRepository.findByUsername("HUTASOIT").orElse(null);
        if(aUser == null && sadmin != null) {
            aUser = new User();
            aUser.setUsername("HUTASOIT");
            aUser.setEmail("ivohutasoit@gmail.com");
            aUser.setPassword("password");
            aUser.setIsLDAPUser(false);
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

        SystemParameter sysparam = sysParamRepository.findByCode("WFO_RADIUS_LIMIT").orElse(new SystemParameter());
        if(sysparam.getId() == null) {
            sysparam.setCode("WFO_RADIUS_LIMIT");
            sysparam.setName("WFO Radius Limit in metre (m)");
            sysparam.setValue("5");
            sysparam.setDecription("Radius limit to indicate that clock in/out is from office");
            sysparam.setIsEditable(true);
            sysparam.setIsSystem(false);
            sysParamRepository.save(sysparam);
        }

        logger.info("[InitialRunner][run] Finish at " + new Date(System.currentTimeMillis()));
    }
    
}
