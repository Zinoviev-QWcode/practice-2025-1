import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class TokenContract extends SmartContract {

    private String tokenName;
    private String tokenSymbol;
    private float totalSupply;
    private Map<PublicKey, Float> balances;
    private Map<PublicKey, Map<PublicKey, Float>> allowances;

    public TokenContract(PublicKey creator, String name, String symbol, float supply) {
        super(creator);
        this.tokenName = name;
        this.tokenSymbol = symbol;
        this.totalSupply = supply;
        this.balances = new HashMap<>();
        this.allowances = new HashMap<>();
        this.balances.put(creator, supply);
    }

    public float balanceOf(PublicKey owner) {
        return balances.getOrDefault(owner, 0f);
    }

    public boolean transfer(PublicKey sender, PublicKey receiver, float amount) {
        if (balanceOf(sender) >= amount) {
            balances.put(sender, balanceOf(sender) - amount);
            balances.put(receiver, balanceOf(receiver) + amount);
            return true;
        }
        return false;
    }

    public boolean approve(PublicKey owner, PublicKey spender, float amount) {
        allowances.computeIfAbsent(owner, k -> new HashMap<>()).put(spender, amount);
        return true;
    }

    public float allowance(PublicKey owner, PublicKey spender) {
        return allowances.getOrDefault(owner, new HashMap<>()).getOrDefault(spender, 0f);
    }

    public boolean transferFrom(PublicKey sender, PublicKey receiver, PublicKey spender, float amount) {
        if (allowance(sender, spender) >= amount && balanceOf(sender) >= amount) {
            balances.put(sender, balanceOf(sender) - amount);
            balances.put(receiver, balanceOf(receiver) + amount);
            allowances.get(sender).put(spender, allowance(sender, spender) - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean execute(Transaction tx, Map<String, TransactionOutput> UTXOs) {
        return true;
    }

    public String getName() { return tokenName; }
    public String getSymbol() { return tokenSymbol; }
    public float getTotalSupply() { return totalSupply; }

}
