package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryOperationResponse {
    private boolean allItemsAdded;
    private String message;

    public boolean isAllItemsAdded() {
        return allItemsAdded;
    }

    public String getMessage() {
        return message;
    }

} 