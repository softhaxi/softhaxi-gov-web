package com.softhaxi.marves.core.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.web.RestfulAuthenticationEntryPoint;
import com.softhaxi.marves.core.web.filter.RestfulRequestFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${app.ldap.host}")
    private String ldapHost;

    @Value("${app.ldap.port}")
    private String ldapPort;

    @Value("${app.ldap.base-dn}")
    private String baseDN;

    @Value("${ldap.username}")
    private String ldapUsername;

    @Value("${ldap.password}")
    private String ldapPassword;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepo;

    @Order(1)
    @Configuration
    public static class RestfulSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value("${app.ldap.host}")
        private String ldapHost;

        @Value("${app.ldap.port}")
        private String ldapPort;

        @Value("${ldap.urls}")
        private String ldapUrl;

        @Value("${ldap.base.dn}")
        private String ldapBaseDN;

        @Value("${ldap.username}")
        private String ldapUsername;

        @Value("${ldap.password}")
        private String ldapPassword;

        @Autowired
        private RestfulAuthenticationEntryPoint authenticationEntryPoint;

        @Autowired
        private RestfulRequestFilter requestFilter;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**").cors().and().csrf().disable().authorizeRequests()
                    .antMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/resetpassword",
                            "/api/v1/upload")
                    .permitAll().anyRequest().authenticated().and().exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint).and().sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
            http.addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter.class);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.ldapAuthentication()
                    //.userDnPatterns("uid={0},ou=people", "uid={0},ou=otherpeople")
                    //.userDnPatterns("uid={0}")
                    .userSearchFilter("(|(uid={0})(mail={0}))")
                    //.groupSearchBase("ou=groups")
                    .contextSource()
                    .url(String.format("%s/%s", ldapUrl, ldapBaseDN))
                    .managerDn(ldapUsername)
                    .managerPassword(ldapPassword);
                    //.and().passwordCompare()
                    // .passwordEncoder(new BCryptPasswordEncoder())
                    //.passwordAttribute("userPassword");
        }

        @Primary()
        @Bean(name = "restAuthenticationManager")
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

    }

    @Order(2)
    @Configuration
    public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Autowired
        @Qualifier("webAuthenticationProvider")
        private WebAuthenticationProvider authenticationProvider;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            //http.antMatcher("/socket/**").cors().and().csrf().disable();
            http.csrf().disable()
                    .authorizeRequests().antMatchers("/", "/webjars/**", "/styles/**", "/scripts/**",
                    "/asset/**", "/socket/**", "/app/**").permitAll()
                    .anyRequest().fullyAuthenticated().and().formLogin().loginPage("/").permitAll()
                    .defaultSuccessUrl("/dashboard").and().logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/?logout")
                    .permitAll();
            http.csrf().disable();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(authenticationProvider);
            // auth.ldapAuthentication()
            // .userDnPatterns("uid={0},ou=people")
            // .groupSearchBase("ou=groups")
            // .contextSource()
            // .url("ldap://localhost:8389/dc=springframework,dc=org")
            // .and().passwordCompare()
            // .passwordEncoder(new BCryptPasswordEncoder())
            // .passwordAttribute("userPassword");
        }

        @Bean(name = "webAuthenticationManager")
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

    }

    @Bean("ldapUserDetailsContextMapper")
    public UserDetailsContextMapper ldapUserDetailsContextMapper() {
        return new LdapUserDetailsMapper() {
            @Override
            public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
                User dUser = userRepository.findByUsernameOrEmailIgnoreCase(username).orElse(null);
                //UserDetails details = super.mapUserFromContext(ctx, username, authorities);
                if(dUser != null) {
                    return super.mapUserFromContext(ctx, dUser.getId().toString(), authorities);
                }
                return super.mapUserFromContext(ctx, username, authorities);
            }
        };
    }

    @Bean("ldapAuthenticationProvider")
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        LdapContextSource context = new LdapContextSource();
        context.setUrls(new String[] { String.format("ldap://%s:%s", ldapHost, ldapPort) });
        context.setBase(baseDN);
        context.setBaseEnvironmentProperties(Collections.unmodifiableMap(new HashMap<>()));
        context.afterPropertiesSet();

        LdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(context, "ou=groups") {
            
            @Transactional
            @Override
            protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {
                User userDao = userRepository
                    .findByUsernameOrEmailIgnoreCase(username)
                    .orElse(null);
                List<Role> roles = (List<Role>) roleRepo.findAllByUser(userDao);
                Set<GrantedAuthority> credentials = null;
                if(userDao != null) {
                    credentials = new HashSet<>();
                    for(var role : roles) {
                        credentials.add(new GrantedAuthority() {

                            /**
                             *
                             */
                            private static final long serialVersionUID = -7640429883875363281L;

                            @Override
                            public String getAuthority() {
                                return role.getName().toUpperCase();
                            }

                        });
                    }
                    return credentials;
                }
                return super.getAdditionalRoles(user, username);
            }
        };
        BindAuthenticator authenticator = new BindAuthenticator(context);
        //authenticator.setUserDnPatterns(new String[] { "uid={0},ou=people" });
        // authenticator.setUserSearch(new FilterBasedLdapUserSearch("",
        // "uid={0},ou=people", context));
        authenticator.setUserSearch(new FilterBasedLdapUserSearch("", "(|(uid={0})(mail={0}))", context));
        authenticator.afterPropertiesSet();

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, populator);
        provider.setUserDetailsContextMapper(ldapUserDetailsContextMapper());

        return provider;
    }

    @Component("webAuthenticationProvider")
    public class WebAuthenticationProvider implements AuthenticationProvider {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        @Qualifier("ldapAuthenticationProvider")
        private LdapAuthenticationProvider ldapAuthProvider;

        @Transactional
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            User user = userRepository
                .findByUsernameOrEmailIgnoreCase(authentication.getName().trim())
                .orElseThrow(() -> new UsernameNotFoundException("error.invalid.credential"));

            var isMobileUser = false;
            if(user.getRoles().size() == 1) {
                for(var userRole : user.getRoles()) {
                    if(userRole.getRole().getName().equalsIgnoreCase("MOBILE")) {
                        isMobileUser = true;
                        break;
                    }
                }
            }

            if(isMobileUser) {
                throw new UsernameNotFoundException("error.invalid.credential");
            }

            if (user.isLDAPUser()) {
                return ldapAuthProvider.authenticate(authentication);
            } else {
                Collection<GrantedAuthority> credentials = new LinkedList<>();
                for(var userRole : user.getRoles()) {
                    credentials.add(new GrantedAuthority() {

                        /**
                         *
                         */
                        private static final long serialVersionUID = -7640429883875363281L;

                        @Override
                        public String getAuthority() {
                            return userRole.getRole().getName().toUpperCase();
                        }

                    });
                }
                if (user.getPassword().equalsIgnoreCase(authentication.getCredentials().toString())) {
                    return new UsernamePasswordAuthenticationToken(user.getId(), user.getPassword(), credentials);
                } else {
                    throw new UsernameNotFoundException("error.invalid.credential");
                }
            }
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }

    @Bean("ldapContextSource")
    public LdapContextSource contextSource() {
        LdapContextSource context = new LdapContextSource();
        context.setUrls(new String[] { String.format("ldap://%s:%s", ldapHost, ldapPort) });
        context.setBase(baseDN);
        context.setUserDn(ldapUsername);
        context.setPassword(ldapPassword);

        return context;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource());
    }
}
