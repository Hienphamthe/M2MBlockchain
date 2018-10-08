package blockchain;

import Utils.StringUtil;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Block {
    private static final Logger LOGGER = LoggerFactory.getLogger(Block.class);
    private int index;
    private String creator;
    private String timestamps;
    private String hash;
    private String previousHash;
    private String merkleRoot;
    private int difficulty;
    private int nonce;
    private int TxAmount;
    public  List<Transaction> transactions = new ArrayList<>(); 

    //Calculate new hash based on blocks contents
    public static String calculateHash(Block block) {
            String record = block.getIndex()
                    + block.getTimestamp() + block.getNonce() 
                    + block.getPrevHash()+ block.getMerkleRoot();
            MessageDigest digest = DigestUtils.getSha256Digest();
            byte[] hash = digest.digest(StringUtils.getBytesUtf8(record));
            return Hex.encodeHexString(hash);
    }

    /**
     * Increases nonce value until hash target is reached.
     * @return hash
     */
    public String mineBlock() {
        this.merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(difficulty); //Create a string with difficulty * "0" 
        hash = calculateHash(this);
        while(!hash.substring( 0, difficulty).equals(target)) {
                nonce ++;
                hash = calculateHash(this);
        }
        LOGGER.info("Block Mined!!! : " + hash);
        return hash;
    }

    /**
     * Validate and add transactions to this block
     * @param transaction
     * @return
     */
    public void addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null){
            LOGGER.error("No transaction to process.");
        }
        if((!"0".equals(previousHash))) {
            if((transaction.processTransaction() != true)) {
                LOGGER.error("Transaction "+transaction.transactionId+" failed to process. Discarded.");               
            }
        }
        transactions.add(transaction);
        LOGGER.info("Transaction Successfully added to Block");
    }

    /**
     * Check the validity of the block
     * 
     * @param newBlock
     * @param oldBlock
     * @return
     */
    public static boolean isBlockValid(Block newBlock, Block oldBlock) {
            if (oldBlock.getIndex() + 1 != newBlock.getIndex()) {
                return false;
            }
            
            if (!oldBlock.getHash().equals(newBlock.getPrevHash())) {
                return false;
            }
            
            for(Transaction tx : newBlock.transactions){
                if (!tx.processTransaction()) {
                    return false;
                }
            }
            
            if (!StringUtil.getMerkleRoot(newBlock.transactions).equals(newBlock.getMerkleRoot())) {
                return false;
            }
            
            if (!calculateHash(newBlock).equals(newBlock.getHash())) {
                return false;
            }
            
            try {
                long deltaDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(newBlock.getTimestamp()).getTime() - 
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(oldBlock.getTimestamp()).getTime();
                if (deltaDate < 0) {
                    return false;
                }
            } catch (ParseException ex) {
                LOGGER.error("New block timestamp is even earlier than the old block.");
            }
            return true;
    }

    /**
     * Block generation
     * 
     * @param oldBlock
     * @param difficulty
     * @param transactions
     * @param creator
     * @return
     */
    public static Block generateBlock(Block oldBlock, int difficulty, List<Transaction> transactions, String creator) {
        Block newBlock = new Block();
        for (Transaction tx : transactions){
            newBlock.addTransaction(tx);
        }
        newBlock.setIndex(oldBlock.getIndex() + 1);
        newBlock.setTxAmount(transactions.size());
        newBlock.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        newBlock.setDifficulty(difficulty);
        newBlock.setPrevHash(oldBlock.getHash());
        newBlock.setCreator(creator);
        newBlock.setHash(newBlock.mineBlock());
        return newBlock;
    }

    @Override
    public boolean equals (Object o) {
        Block block = (Block) o;
        return (block.hash == null ? this.hash == null : block.hash.equals(this.hash));
    }
    
    /** getters and setters**/
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public int getTxAmount() {
        return TxAmount;
    }
    public void setTxAmount(int amount) {
        this.TxAmount = amount;
    }
    public String getTimestamp() {
        return timestamps;
    }
    public void setTimestamp(String timestamp) {
        this.timestamps = timestamp;
    }
    public String getHash() {
        return hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
    public String getPrevHash() {
        return previousHash;
    }
    public void setPrevHash(String prevHash) {
        this.previousHash = prevHash;
    }
    public int getDifficulty() {
        return difficulty;
    }
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
    public int getNonce() {
        return nonce;
    }
    public void setNonce(int nonce) {
        this.nonce = nonce;
    }
    public String getMerkleRoot() {
        return merkleRoot;
    }
    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }
    public List<Transaction> getTx() {
        return transactions;
    }
}
