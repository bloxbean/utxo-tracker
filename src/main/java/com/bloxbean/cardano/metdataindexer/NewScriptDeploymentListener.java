package com.bloxbean.cardano.metdataindexer;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.utxo.domain.Address;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import com.bloxbean.cardano.yaci.store.utxo.storage.AddressStorage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NewScriptDeploymentListener is an event listener responsible for subscribing to {@code AddressUtxoEvent}
 * and processing transaction input and output data to identify relevant script references.
 * This class derives script-based addresses, filters specific transactions,
 * and stores those addresses using the {@code AddressStorage} instance.
 *
 * The listener looks for specific conditions, such as script references and relevant policy IDs,
 * and processes addresses by deserializing the script references and constructing address information
 * including payment credentials and stake addresses.
 *
 * Components:
 * - AddressStorage: Service responsible for persistent storage of derived addresses.
 * - Configuration values: Configurations such as {@code scriptDeployAddress}, {@code policyId},
 *   and the network type (mainnet/testnet) are used to guide the logic.
 *
 * Main Tasks:
 * 1. Filter transaction outputs associated with a specific script deployment address and policy ID.
 * 2. Deserialize script references and derive base addresses and stake addresses.
 * 3. Persist the filtered and processed address data.
 *
 * Methods:
 * - {@code handleAddressUtxoEventForNewScriptDeployment(AddressUtxoEvent event)}:
 *   The main method that processes the AddressUtxoEvent, filters transaction inputs,
 *   and derives, validates, and stores addresses.
 */
@Component
@Slf4j
public class NewScriptDeploymentListener {
    private final AddressStorage addressStorage;
    private final AddressCache addressCache;

    @Value("${script.deploy.address}")
    private String scriptDeployAddress;

    @Value("${policy_id}")
    private String policyId;

    @Value("${script.delegation.hash}")
    private String scriptDelegationHash;

    @Value("${is_mainnet:false}")
    private boolean isMainnet;

    private Credential scriptDelegationCredential;

    public NewScriptDeploymentListener(AddressStorage addressStorage, AddressCache addressCache) {
        this.addressStorage = addressStorage;
        this.addressCache = addressCache;
    }

    @PostConstruct
    public void init() {
        this.scriptDelegationCredential = Credential.fromScript(scriptDelegationHash);
    }

    @EventListener
    public void handleAddressUtxoEventForNewScriptDeployment(AddressUtxoEvent event) {
        List<TxInputOutput> txInOuts = event.getTxInputOutputs();

        var scriptRefs = txInOuts.stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(addressUtxo -> addressUtxo.getOwnerAddr().equals(scriptDeployAddress))
                .filter(addressUtxo -> {
                    // Check if the addressUtxo has a specific amount
                    return addressUtxo.getAmounts().stream()
                            .anyMatch(amount -> policyId.equals(amount.getPolicyId()));
                }).filter(addressUtxo -> addressUtxo.getScriptRef() != null)
                .map(addressUtxo -> addressUtxo.getScriptRef()).toList();

        if (scriptRefs.size() == 0)
            return;

        //Derive address
        List<Address> scriptAddresses = scriptRefs.stream()
                .map(scriptRef -> {

                    com.bloxbean.cardano.client.spec.Script script;
                    try {
                        script = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(scriptRef));
                    } catch (Exception e) {
                        return null;
                    }

                    if (!(script instanceof PlutusV3Script)) {
                        return null;
                    }

                    Credential paymentCredential = null;
                    try {
                        var paymentHash = script.getScriptHash();
                        paymentCredential = Credential.fromScript(paymentHash);
                    } catch (Exception e) {
                        log.error("Error getting script hash: {}", scriptRef, e);
                        return null;
                    }

                    var baseAddress = AddressProvider.getBaseAddress(paymentCredential, scriptDelegationCredential, isMainnet ? Networks.mainnet() : Networks.testnet());

                    var stakeAddress = AddressProvider.getRewardAddress(script, isMainnet? Networks.mainnet() : Networks.testnet());
                    var stakeCredential = HexUtil.encodeHexString(stakeAddress.getDelegationCredentialHash().orElse(null));

                    Address address = Address.builder()
                            .address(baseAddress.toBech32())
                            .stakeAddress(stakeAddress.toBech32())
                            .paymentCredential(HexUtil.encodeHexString(paymentCredential.getBytes()))
                            .stakeCredential(stakeCredential)
                            .slot(event.getEventMetadata().getSlot())
                            .build();

                    return address;
                }).filter(address -> address != null)
                .toList();

        if (scriptAddresses.size() == 0)
            return;

        addressCache.addAddressToCache(scriptAddresses); //add to cache
        addressStorage.save(scriptAddresses); // save to DB
    }

    //We don't need rollback here as rollback method for address table will be used in default AddressProcessor class
    //in Yaci Store
}
