package com.codinglemonsbackend.Config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
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
            }catch(ExpiredJwtException e){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"JWT token has expired\",\"message\":\"Please login again\"}");
                //publishUnauthorizedEvent(request, "JWT_EXPIRED");
                return;
            } 
            catch (SignatureException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid JWT signature\",\"message\":\"Token signature verification failed\"}");
                //publishUnauthorizedEvent(request, "INVALID_SIGNATURE");
                return;
            } 
            catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid JWT token\",\"message\":\"Token processing failed\"}");
                //publishUnauthorizedEvent(request, "JWT_PROCESSING_ERROR");
                return;
            }
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userService.loadUserByUsername(userName);
            if (jwtUtils.validateToken(jwt, userDetails, ((UserEntity)userDetails).getPasswordIssueDate())){
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
                );
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"JWT token has expired\",\"message\":\"Please login again\"}");
                //OkaypublishUnauthorizedEvent(request, "TOKEN_VALIDATION_FAILED");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
    
}
