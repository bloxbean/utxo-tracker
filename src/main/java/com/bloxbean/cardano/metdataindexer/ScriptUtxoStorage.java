package com.bloxbean.cardano.metdataindexer;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * ScriptUtxoStorage is a specialized implementation of UtxoStorageImpl
 * that filters and stores unspent transaction outputs (UTXOs) for script-based addresses.
 * It integrates with an AddressCache to validate addresses before saving UTXOs.
 *
 * The saveUnspent method is overridden to filter UTXOs based on specific conditions
 * before saving them. The filtering currently includes:
 * - Address existence validation using the AddressCache.
 *
 */
@Component
@Slf4j
public class ScriptUtxoStorage extends UtxoStorageImpl {
    private final AddressCache addressCache;

    @Value("${policy_id}")
    private String policyId;

    public ScriptUtxoStorage(UtxoRepository utxoRepository,
                             TxInputRepository spentOutputRepository,
                             DSLContext dsl,
                             UtxoCache utxoCache,
                             PlatformTransactionManager transactionManager,
                             AddressCache addressCache) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, transactionManager);
        this.addressCache = addressCache;
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        // Filter based on existing script address and policy ID
        List<AddressUtxo> filteredUtxos = addressUtxoList.stream()
                .filter(addressUtxo -> addressCache.addressExists(addressUtxo.getOwnerAddr()))
//                .filter(addressUtxo -> addressUtxo.getAmounts().stream()     //Policy id based filter commented for now
//                        .filter(amount -> policyId.equals(amount.getPolicyId())).count() > 0)
                .toList();

        super.saveUnspent(filteredUtxos);
    }
}
