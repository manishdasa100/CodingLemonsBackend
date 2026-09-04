package com.codinglemonsbackend.Entities;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.codinglemonsbackend.Dto.AuthProvider;
import com.codinglemonsbackend.Dto.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Users")
@JsonIgnoreProperties(value = {"password"}, allowSetters = true)
@CompoundIndex(name = "auth_provider_lookup", def = "{'authProviderId': 1, 'authProvider': 1}")
public class UserEntity implements UserDetails{
    
    @Id
    private String username;

    private String password;

    private Date passwordIssueDate;

    private Role role;

    @Indexed(unique = true, sparse = true)
    private String email;
    
    private String zoneId;

    private String authProviderId;

    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
       return true;
    }
    @Override
    public boolean isEnabled() {
        return true;
    }
}
