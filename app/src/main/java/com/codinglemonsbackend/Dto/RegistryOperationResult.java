package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryOperationResult {
    private Integer problemId;
    private String registryId;
    private Boolean allItemsProcessed;
    private String message;

    public Integer getProblemId() {
        return problemId;
    }

    public String getRegistryId() {
        return registryId;
    }

    public Boolean isAllItemsProcessed() {
        return allItemsProcessed;
    }

    public String getMessage() {
        return message;
    }

} 