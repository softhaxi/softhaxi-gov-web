package com.softhaxi.marves.core.runner;

import java.util.Date;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.access.UserRole;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.access.UserRoleRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;

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

        logger.info("[InitialRunner][run] FInish at " + new Date(System.currentTimeMillis()));
    }
    
}
