package com.codinglemonsbackend.Config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Service.UserService;
import com.codinglemonsbackend.Utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authorizaitionHeader = request.getHeader("Authorization"); 
        
        String userName=null;
        String jwt=null;

        if (authorizaitionHeader != null && authorizaitionHeader.startsWith("Bearer ")) {
            jwt = authorizaitionHeader.substring(7);
            try{
                userName = jwtUtils.extractUsername(jwt);
                System.out.println("JWT AUTH FILTER ==> USERNAME = " + userName);
            }catch(ExpiredJwtException e){
                // exceptionResolver.resolveException(request, response, null, e);
            } 
            catch (SignatureException e) {
                // exceptionResolver.resolveException(request, response, null, e);
            } 
            catch (Exception e) {
                // exceptionResolver.resolveException(request, response, null, e);
            }
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("User logging in");
            UserDetails userDetails = this.userService.loadUserByUsername(userName);
            if (jwtUtils.validateToken(jwt, userDetails, ((UserEntity)userDetails).getPasswordIssueDate())){
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
                );
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
    
}
