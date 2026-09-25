package com.codinglemonsbackend.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A queued click. It carries no timestamp: ordering within a batch comes from the Redis stream id,
 * which one server assigns monotonically, rather than from a clock on whichever instance took the
 * request. Unknown fields are ignored so events already queued in the old shape still parse.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LikeEvent {

    private Integer problemId;

    private String username;

    private Boolean isLike;
}
