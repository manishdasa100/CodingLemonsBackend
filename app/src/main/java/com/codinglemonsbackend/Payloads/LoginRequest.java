package com.codinglemonsbackend.Payloads;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @NotEmpty
    private String username;

    @NotEmpty
    private String password;
}
