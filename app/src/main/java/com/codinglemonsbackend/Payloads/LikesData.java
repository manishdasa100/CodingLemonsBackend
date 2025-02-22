package com.codinglemonsbackend.Payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikesData {
    
    private String totalLikes;

    private Boolean problemLikedByUser;

}
