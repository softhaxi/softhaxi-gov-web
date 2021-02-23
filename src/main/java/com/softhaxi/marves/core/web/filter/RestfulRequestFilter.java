package com.softhaxi.marves.core.web.filter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.softhaxi.marves.core.domain.logging.Session;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
import com.softhaxi.marves.core.util.AccessTokenUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Component
public class RestfulRequestFilter extends OncePerRequestFilter {

    @Autowired
    private AccessTokenUtil accessTokenUtil;

    @Autowired
    private SessionRepository sessionRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if(request.getRequestURI().startsWith("/api") 
            && !request.getRequestURI().contains("/auth/login")
            && !request.getRequestURI().contains("/auth/register")
            && !request.getRequestURI().contains("/auth/resetpassword")) {
            final String requestTokenHeader = request.getHeader("Authorization");

            String userid = null;
            String token = null;

            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            if(requestTokenHeader != null && requestTokenHeader.startsWith("Bearer")) {
                token = requestTokenHeader.substring(7);
                try {
                    Session session = sessionRepo.findOneValidByAccessToken(token).orElseThrow();
                    session.setLastUsed(ZonedDateTime.now());
                    sessionRepo.save(session);
                    userid = accessTokenUtil.getUsernameFromToken(session.getAccessToken());
                } catch(IllegalArgumentException iaex) {
                    logger.error("Unable to get token", iaex);
                } catch (ExpiredJwtException ejwtex) {
                    logger.error("Token has expired", ejwtex);
                } catch(Exception ex) {
                    logger.error("Exception ex", ex);
                }
            } else {
                logger.warn("Invalid access token format");
            }

            if(userid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if(accessTokenUtil.validateToken(token, userid)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userid, null, grantedAuthorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
    
}
