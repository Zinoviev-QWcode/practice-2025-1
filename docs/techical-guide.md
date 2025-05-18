# Создание простого блокчейна на Java
## Что такое блокчейн?
Блокчейн - это цепочка/список блоков. Каждый такой блок в блокчейне имеет свой собсвтенный цифровой отпечаток, цифровой отпечаток предыдущего блока 
, а также имеет некоторые данные (Например, транзакции). Хэш - цифровой отпечаток. Каждый блок не просто содержит хеш предшествующего ему блока, но и его 
собственный хэш частично вычисляется из предыдущего хэша. При изменении данных предыдущего блока меняется хэш, что в свою очередь повляет на все хэши последующих блоков. 
Это значит, что изменение любых данных в этом списке изменит подпись и розорвет цепочку.
## Создание блокчейна
### При создании блокечйна потребуются:
- Язык программирования Java
- Среда разработки Intellij IDEA
- Установленный Open JDK-20

### Создание проекта
Для этого открываем среду Intellij IDEA, нажимаем `File -> New -> Project`. В открывшемся окне выбираем проект типа Java. В поле Project SDK 
выбираем openjdk-20. Нажимаем Next, тем самым переходя к следующему окну, в котором вписываем название нашего проекта и выбираем директорию, 
в которой будет храниться проект.
## Написание кода
Для того, чтобы начать писать код, нужно создать первый класс. Назвоем его Block он будет составлять наш блокчейн. 
```java
public class Block {
    
    public String hash;
    public String previousHash;
    private long timeStamp;
    private int nonce;

    public Block(String previousHash ) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash();
    }
}
```
В данном классе String hash будет хранить цифровую подпись, previousHash - хранение предыдущего блока.
Далее в качестве способа создания криптографического отпечатка выберем SHA256, для этого его нужно импортировать
`import java.security.MessageDigest;`
Напишем вспомогательный метод в новом классе - `StringUtil.java`. Его код представлен ниже:
```java
import java.security.*;

public class StringUtil {

    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++){
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
```
Теперь воспользуемся этим методом в классе `Block.java`. 
```java
public String calculateHash(){
        String calculatedhash = StringUtil.applySha256(
                previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + merkleRoot);
        return calculatedhash;
    }
```
Теперь создадим главный класс, который будет содержать метод main. Именно его мы будем запускать для проверки работоспособности программы.
Назовем его `Blockchain.java`.
В этом классе создадим метод, который будет проверять верность цепочки блоков в блокчейне.
```java
public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;

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
return true;
}
```
Напишем код для метода, который будет добывать блок
```java
public void mineBlock(int difficulty) {
        String target = new String (new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce ++;
            hash = calculateHash();
        }
        System.out.println("Block mined! : " + hash);
    }
```
Метод `mineBlock()` принимает int с именем difficulty, это количество нулей, которые нужно решить. Низкая сложность (1-2) может быть решена мгновенно на большинстве компьютеров. Для примера будем использовать сложность 5. 
Добавим сложность как статическую переменную в `Blockchain.java` класс
```java
public static int difficulty = 3;
```
## Транзакции
Для того, чтобы перейти к следующему этапу написания кода, нам потребуется импортировать зависимости:
- bounceycastle
- GSON

Для этого в Intellij IDEA переходим в `File -> Project Structure -> Libraries`. В открытом окне нажимаем на плюс, и из развернутого списка выбираем `From Maven`. Затем в поле поиска вводим название нужной нам зависимости и устанавливаем ее (***Для корректной работы требуется подключение к интернету***).

## Подготовка кошелька
В криптовалютах право собственности на монеты передается в блокчейне в виде транзакций. Участники имеют адрес, на который и с которого можно отправлять средства.
Перейдем к созданию класса кошелька.
```java
public class Wallet {

    public PrivateKey privateKey;
    public PublicKey publicKey;
}
```
Мы создали закрытый и открытый ключи. Открытый ключ служит в качестве нашего адреса. Закрытый применяется для подписи транзакций.
Далее для генерации ключей мы будем использовать криптографию эллиптических кривых. Добавим метод для генерации ключей, а также вызовем его в конструкторе класса `Wallet.java`.
```java
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
```

### Транзакции и подписи
Каждая транзакция будет хранить:
- Открытый ключ отправителя
- Открытый ключ получателя
- Сумма переводимых средств
- Входные данные (ссылки на предыдущие транзакции отправителя)
- Криптографическая подпись

Создадим класс транзакции
```java
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {

    public String transactionId;
    public PublicKey sender;
    public PublicKey reciepient;
    public float value;
    public byte[] signature;

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0;

    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }
}
```
Также потребуется создать пустые классы TransactionInput и TransactionOutput.
Далее создадим методы для генерации и подтверждения подписи
```java
public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        signature = StringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }
```
Допишем класс TransactionInput. Этот класс будет сипользоваться для ссылки на TransactionOutputs, которые еще не были потрачены.
transactionOutputId будет использоваться для поиска соответсвующего TransactionOutput, позволяя майнерам проверять право собственности.
```java
public class TransactionInput {
    public String transactionOutputId;
    public TransactionOutput UTXO;

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
```
А также класс TransactionOutput:
```java
import java.security.PublicKey;

public class TransactionOutput {
    public String id;
    public PublicKey reciepient;
    public float value;
    public String parentTransactionId;

    public TransactionOutput(PublicKey reciepient, float value, String parentTransactionId) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient)+Float.toString(value)+parentTransactionId);
    }

    public boolean isMine(PublicKey publicKey) {
        return (publicKey == reciepient);
    }

}
```
На данном этапе код был взят из открытого репозитория на GitHub. Теперь перейдем к следующему этапу - модификации текущей версии кода.
## Доработка кода
Для данного этапа было принято решение реализовать PoS (Proof of Stake) и DPoS (Delegated Proof of Stake). А также будут написаны Smart-контракты подобные ERC-20.
### Что такое PoS и DPoS?
Это два консенсусных алгоритма, которые заменяют энергозатратный PoW (как в Bitcoin). Они решают проблему масштабируемости и централизации майнинга.
В PoS:
- Валидаторы блокируют монеты в сети.
- Чем больше монет заблокировано, тем выше шанс создать блок.
- Нет майнинга. Блоки создаются через детерминированный или случайный выбор валидатора.

В DPoS:
- Монетодержатели голосуют за делегатов.
- Топ-N делегатов (например, 21 в EOS) по очереди создают блоки.
- Делегаты получают награды и делятся ими с избирателями.

Начнем с того, что создадим класс делегатов:
```java
public class Delegate {
    public Wallet wallet;
    public int votes;

    public Delegate(Wallet wallet, int votes) {
        this.wallet = wallet;
        this.votes = votes;
    }
}
```
В классе Block создадим 2 метода, они будут отвечать за добычу блока в алгоритмах PoS и DPoS:
```java
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
```
Для класса Wallet добавим 2 метода. А также поле stake, в котором будут храниться замороженные монеты.
```java
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
```
Помимо этого в главном классе нужно реализовать метод для выбора валидатора с наибольшим stake.
```java
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
```
Затем вернемся в класс Wallet и напишем метод для DPoS, который отвечает за голосование:
```java

    public void voteForDelegate(Delegate delegate, int votes) {
        if (stake >= votes) {
            delegate.votes += votes;
            votesGiven.put(delegate, votesGiven.getOrDefault(delegate, 0) + votes);
            stake -= votes;
            System.out.println("Voted for delegate: " + votes + " votes");
        }
    }
```
Реализуем выбор делегатов в Blockchain
```java
public static List<Delegate> selectDelegates(List<Wallet> wallets, int count) {
        List<Delegate> delegates = wallets.stream()
                .map(w -> new Delegate(w, (int) w.stake))
                .sorted((d1, d2) -> d2.votes - d1.votes)
                .limit(count)
                .collect(Collectors.toList());
        return delegates;
    }
```
### Smart-контракты
Для начала создадим абстрактный класс в Java, который мы будем наследовать при написании других классов:
```java
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
```
Теперь создадим класс TokenContract который расширяется за счет SmartConctract с помощью ключего слова `extends` в Java.
```java
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
```
## Тестирование
Тест будет проходить в консоли среды Intellij IDEA. Для этого в классе Blockchain метода main напишем следующий функционал:
- Создание блока:
```java
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Genesis block has been created! Hash: " + genesis.hash);
        System.out.println("Balance of WalletA: " + walletA.getBalance());
```
- Обычная транзакция (PoW)
```java
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
```
- Тестирование PoS
```java
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
```
- Тестирование DPoS
```java
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
```
- Smart-контракты
```java
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
```

После того как весь этот код был написан, можно запустить программу, нажав на зеленый треугольник. В консоли отобразится результат выполнения кода. 
В данном техническом руководстве расписаны основные этапы выполнения работы с заострением внимания на важных деталях кода. С более подробным кодом по данной работе можно ознакомиться в текущем репозитории в директории `practice2025-1/src/...`
