import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.*;
import java.util.stream.Collectors;

public class Blockchain {

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA;
    public static Wallet walletB;
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("=== Initialization of the Blockchain ===");

        walletA = new Wallet();
        walletB = new Wallet();
        Wallet coinbase = new Wallet();
        System.out.println("\n[Wallets has been created]");
        System.out.println("Coinbase: " + StringUtil.getStringFromKey(coinbase.publicKey));
        System.out.println("WalletA: " + StringUtil.getStringFromKey(walletA.publicKey));
        System.out.println("WalletB: " + StringUtil.getStringFromKey(walletB.publicKey));

        System.out.println("\n=== Creating genesis block ===");
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 1000f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId));
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        System.out.println("\n[Mining genesis block...]");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Genesis block has been created! Hash: " + genesis.hash);
        System.out.println("Balance of WalletA: " + walletA.getBalance());

        System.out.println("\n=== Common transaction (PoW) ===");
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA is sending 40 coins to WalletB");
        Transaction tx1 = walletA.sendFunds(walletB.publicKey, 40f);
        block1.addTransaction(tx1);
        addBlock(block1);
        System.out.println("Transaction has been completed! Block hash: " + block1.hash);
        System.out.println("Balances:");
        System.out.println("WalletA: " + walletA.getBalance());
        System.out.println("WalletB: " + walletB.getBalance());

        //Proof of Stake
        System.out.println("\n=== Proof of Stake ===");
        System.out.println("\nWalletA is staking 100 coins");
        walletA.stakeCoins(100);
        System.out.println("WalletB is staking 50 coins");
        walletB.stakeCoins(50);

        System.out.println("\n[Choosing validator...]");
        Wallet posValidator = selectValidator(Arrays.asList(walletA, walletB));
        System.out.println("Validator has been choose: " + StringUtil.getStringFromKey(posValidator.publicKey));

        Block posBlock = new Block(block1.hash);
        System.out.println("\n[Creating PoS-block...]");
        posBlock.mineBlockPos(posValidator);
        addBlock(posBlock);
        System.out.println("PoS-block has been created by validator! Hash: " + posBlock.hash);

        //DPoS
        System.out.println("\n=== Delegated Proof of Stake ===");
        List<Delegate> delegates = new ArrayList<>();
        delegates.add(new Delegate(walletA,0));
        delegates.add(new Delegate(walletB,0));
        System.out.println("\nDelegates was created:");
        System.out.println("1. " + StringUtil.getStringFromKey(walletA.publicKey));
        System.out.println("2. " + StringUtil.getStringFromKey(walletB.publicKey));

        System.out.println("\n[Voting...]");
        System.out.println("WalletA is voting for Delegate1 (30 votes)");
        walletA.voteForDelegate(delegates.get(0), 30);
        System.out.println("WalletB is voting for Delegate1 (20 votes)");
        walletB.voteForDelegate(delegates.get(0), 20);

        System.out.println("\n[Choosing the delegates...]");
        List<Delegate> topDelegates =  selectDelegates(Arrays.asList(walletA, walletB), 1);
        System.out.println("Top delegate: " + StringUtil.getStringFromKey(topDelegates.get(0).wallet.publicKey) +
                " (votes: " + topDelegates.get(0).votes + ")");

        System.out.println("\n[Creating DPoS-block...]");
        Block dposBlock = new Block(posBlock.hash);
        dposBlock.mineBlockDpos(topDelegates.get(0));
        addBlock(dposBlock);
        System.out.println("DPoS-block has been created by delegate! hash: " + dposBlock.hash);

        //ERC-20 токен
        System.out.println("\n=== Token ERC-20 ===");
        TokenContract token = new TokenContract(walletA.publicKey, "DPoS Coin", "DPOS", 10000f);
        Blockchain.deployContract(token);
        System.out.println("\nToken was created: " + token.getName() + " (" + token.getSymbol() + ")");
        System.out.println("Total supply: " + token.getTotalSupply());
        System.out.println("Balance of WalletA: " + token.balanceOf(walletA.publicKey));

        System.out.println("\nWalletA is sending 500 tokens for WalletB");
        token.transfer(walletA.publicKey, walletB.publicKey, 500f);
        System.out.println("Tokens have been transferred!");
        System.out.println("Balances of tokens:");
        System.out.println("WalletA: " + token.balanceOf(walletA.publicKey));
        System.out.println("WalletB: " + token.balanceOf(walletB.publicKey));

        System.out.println("\n=== Check blockchain ===");
        boolean isValid = isChainValid();

        System.out.println("\n=== Final balances ===");
        System.out.println("WalletA: " + walletA.getBalance() + " coins | " +
                token.balanceOf(walletA.publicKey) + " tokens");
        System.out.println("WalletB: " + walletB.getBalance() + " coins | " +
                token.balanceOf(walletB.publicKey) + " tokens");
    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>();
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));


        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);

            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }

            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }

            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }

    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }

    public static Wallet selectValidator(List<Wallet> wallets) {
        Wallet validator = null;
        float maxStake = 0;
        for (Wallet wallet : wallets) {
            if (wallet.stake > maxStake) {
                maxStake = wallet.stake;
                validator = wallet;
            }
        }
        return validator;
    }

    public static List<Delegate> selectDelegates(List<Wallet> wallets, int count) {
        List<Delegate> delegates = wallets.stream()
                .map(w -> new Delegate(w, (int) w.stake))
                .sorted((d1, d2) -> d2.votes - d1.votes)
                .limit(count)
                .collect(Collectors.toList());
        return delegates;
    }

    public static Map<String, SmartContract> contracts = new HashMap<>();

    public static void deployContract(SmartContract contract) {
        contracts.put(contract.contractAddress, contract);
    }

}