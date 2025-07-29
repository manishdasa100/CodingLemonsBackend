package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Repository.IRegistryService;


@Service
public class RegistryServiceDispatcher {

    private Map<String, IRegistryService<?>> services;

    @Autowired
    public RegistryServiceDispatcher(List<IRegistryService<?>> registryServices) {
        services = registryServices.stream()
            .collect(Collectors.toMap(
                IRegistryService::getRegistryType,
                Function.identity()
            ));
    }

    public IRegistryService<?> getService(String registryType) {
        IRegistryService<?> service = services.get(registryType);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported registry type: " + registryType);
        }
        return service;
    }

    public List<String> getSupportedRegistryTypes() {
        return services.keySet().stream().collect(Collectors.toList());
    }
}
