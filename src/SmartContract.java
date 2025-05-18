import java.security.PublicKey;
import java.util.Map;

public abstract class SmartContract {

    protected PublicKey creator;
    protected String contractAddress;

    public SmartContract(PublicKey creator) {
        this.creator = creator;
        this.contractAddress = StringUtil.applySha256(
                StringUtil.getStringFromKey(creator) + System.currentTimeMillis()
        );
    }

    public abstract boolean execute(Transaction tx, Map<String, TransactionOutput> UTXOs);

}
