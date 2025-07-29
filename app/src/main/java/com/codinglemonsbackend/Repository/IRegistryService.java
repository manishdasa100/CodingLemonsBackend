package com.codinglemonsbackend.Repository;

import java.util.Optional;
import com.codinglemonsbackend.Dto.RegistryOperationResult;

public interface IRegistryService<T> {

    String getRegistryType();
    
    Optional<T> getRegistry(Integer problemId);

    Optional<T> getRegistry(String registryId);

    void saveRegistry(T registry);

    RegistryOperationResult addItemsInRegistry(Integer problemId, Object data);

    RegistryOperationResult updateItemsInRegistry(String registryId, Object data);

    RegistryOperationResult removeItemFromRegistry(String registryId, Object data);

    RegistryOperationResult deleteRegistry(String registryId);
}
