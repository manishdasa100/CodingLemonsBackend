package com.codinglemonsbackend.Payloads;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequestPayload {
    
    @NotNull
    String username;

    @NotNull
    String newPassword;
}
