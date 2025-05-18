import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

public class Block {
    
    public String hash;
    public String previousHash;
    public String merkleRoot;
    private long timeStamp;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    private int nonce;

    public PublicKey validatorPublicKey;

    public Block(String previousHash ) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash();
    }

    public String calculateHash(){
        String calculatedhash = StringUtil.applySha256(
                previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + merkleRoot);
        return calculatedhash;
    }

    public void mineBlock(int difficulty) {
        String target = new String (new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce ++;
            hash = calculateHash();
        }
        System.out.println("Block mined! : " + hash);
    }

    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null) return false;
        if((!"0".equals(previousHash))) {
            if((transaction.processTransaction() != true)) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

    public void mineBlockPos(Wallet validator) {
        this.validatorPublicKey = validator.publicKey;
        this.hash = calculateHash();
        System.out.println("Block validated by: " + StringUtil.getStringFromKey(validator.publicKey));
    }

    public void mineBlockDpos(Delegate delegate) {
        this.validatorPublicKey = delegate.wallet.publicKey;
        this.hash = calculateHash();
        System.out.println("Block created by delegate: " + delegate.wallet.publicKey);
    }

}
