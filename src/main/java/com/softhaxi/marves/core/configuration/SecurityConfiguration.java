package com.softhaxi.marves.core.configuration;

import com.softhaxi.marves.core.authentication.CustomAuthenticationProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {


    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Order(1)
    @Configuration
    public static class RestfulSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/api/**")
                .cors()
                    .and()
                .csrf()
                    .disable() // we don't need CSRF because our token is invulnerable
                .authorizeRequests()
                    .antMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/resetpassword").permitAll()
                    .anyRequest().authenticated()
                    .and()
                // .addFilter(new JWTAuthenticationFilter(authenticationManager()))
                // .addFilter(new JWTAuthorizationFilter(authenticationManager()))
                // this disables session creation on Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.ldapAuthentication().userDnPatterns("uid={0},ou=people").groupSearchBase("ou=groups").contextSource()
                    .url("ldap://localhost:8389/dc=springframework,dc=org").and().passwordCompare()
                    .passwordEncoder(new BCryptPasswordEncoder()).passwordAttribute("userPassword");
        }

        @Bean(name = "restAuthenticationManager")
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }
        
    }

    @Order(2)
    @Configuration
    public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
            .authorizeRequests()
            .antMatchers("/", "/styles/*", "/login").permitAll()
            .anyRequest()
            .fullyAuthenticated().and().formLogin().loginPage("/");
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.ldapAuthentication().userDnPatterns("uid={0},ou=people").groupSearchBase("ou=groups").contextSource()
                    .url("ldap://localhost:8389/dc=springframework,dc=org").and().passwordCompare()
                  //  .passwordEncoder(new BCryptPasswordEncoder())
                  .passwordAttribute("userPassword");
        }

        @Bean(name = "webAuthenticationManager")
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

    }
}
