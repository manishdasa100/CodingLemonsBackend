package com.codinglemonsbackend.Repository;

import java.util.Optional;
import com.codinglemonsbackend.Dto.RegistryOperationResponse;

public interface IRegistryService<T> {

    String getRegistryType();
    
    Optional<T> getRegistry(Integer problemId);

    Optional<T> getRegistry(String registryId);

    void saveRegistry(T registry);

    RegistryOperationResponse addItemsInRegistry(Integer problemId, Object data);

    RegistryOperationResponse updateItemsInRegistry(String registryId, Object data);

    RegistryOperationResponse removeItemFromRegistry(String registryId, Object data);

    void deleteRegistry(String registryId);
}
