import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Wallet {

    public PrivateKey privateKey;
    public PublicKey publicKey;
    public float stake;

    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    private Map<Delegate, Integer> votesGiven = new HashMap<>();

    public Wallet() {
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");

            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item: Blockchain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) {
                UTXOs.put(UTXO.id,UTXO);
                total += UTXO.value ;
            }
        }
        return total;
    }

    public Transaction sendFunds(PublicKey _recipient,float value ) {
        if(getBalance() < value) {
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }
        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        float total = 0;
        for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total > value) break;
        }

        Transaction newTransaction = new Transaction(publicKey, _recipient , value, inputs);
        newTransaction.generateSignature(privateKey);

        for(TransactionInput input: inputs){
            UTXOs.remove(input.transactionOutputId);
        }

        return newTransaction;
    }

    //Methods for Proof of Stake (PoS)
    public void stakeCoins(float amount) {
        if (getBalance() >= amount) {
            stake += amount;

            for (TransactionOutput utxo : new ArrayList<>(UTXOs.values())) {
                if (amount <= 0) break;
                float valueToLock = Math.min(utxo.value, amount);
                Blockchain.UTXOs.remove(utxo.id);
                amount -= valueToLock;
            }
            System.out.println("Staked: " + stake + " | New balance: " + getBalance());
        } else {
            System.out.println("Not enough balance to stake!");
        }
    }

    public void unstakeCoins(float amount) {
        if (stake >= amount) {
            stake -= amount;

            TransactionOutput newUtxo = new TransactionOutput(this.publicKey, amount, "unstake_" + UUID.randomUUID());
            Blockchain.UTXOs.put(newUtxo.id, newUtxo);
            System.out.println("Unstaked: " + amount + " | New stake: " + stake);
        }
    }

    //Methods for DPos
    public void voteForDelegate(Delegate delegate, int votes) {
        if (stake >= votes) {
            delegate.votes += votes;
            votesGiven.put(delegate, votesGiven.getOrDefault(delegate, 0) + votes);
            stake -= votes;
            System.out.println("Voted for delegate: " + votes + " votes");
        }
    }

}
