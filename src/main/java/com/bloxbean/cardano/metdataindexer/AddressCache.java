package com.bloxbean.cardano.metdataindexer;

import com.bloxbean.cardano.yaci.store.utxo.domain.Address;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.AddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AddressCache {
    private final AddressRepository addressRepository;
    private final Set<String> addressCache = ConcurrentHashMap.newKeySet();

    public AddressCache(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
        refreshCache();
        log.info("Address cache initialized with {} addresses", addressCache.size());
    }

    public boolean addressExists(String address) {
        synchronized (addressCache) {
            return addressCache.contains(address);
        }
    }

    public void addAddressToCache(String address) {
        synchronized (addressCache) {
            addressCache.add(address);
        }
    }

    public void addAddressToCache(List<Address> addresses) {
        synchronized (addressCache) {
            var addressStrList = addresses.stream()
                            .map(address -> address.getAddress())
                                    .collect(Collectors.toSet());

            addressCache.addAll(addressStrList);
        }
    }

    public void refreshCache() {
        synchronized (addressCache) {
            addressCache.clear();
            addressCache.addAll(addressRepository.findAll().stream()
                .map(addressEntity -> addressEntity.getAddress())
                .toList());
        }
    }
}
