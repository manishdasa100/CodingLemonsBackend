package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Set;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryOperationResult {
    private Integer problemId;
    private Boolean allItemsProcessed;
    private List<?> ignoredAdditions;
    private List<?> ignoredUpdates;
    private Set<String> ignoredDeletions;

    public Integer getProblemId() {
        return problemId;
    }

    public Boolean isAllItemsProcessed() {
        return allItemsProcessed;
    }

    public List<?> getIgnoredAdditions() {
        return ignoredAdditions;
    }

    public List<?> getIgnoredUpdates() {
        return ignoredUpdates;
    }

    public Set<String> getIgnoredDeletions() {
        return ignoredDeletions;
    }
}
