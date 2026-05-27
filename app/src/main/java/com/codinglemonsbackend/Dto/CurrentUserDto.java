package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserDto {

    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
}
