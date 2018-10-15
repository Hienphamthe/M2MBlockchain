package blockchain;
import Utils.StringUtil;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transaction {
        private static final Logger LOGGER = LoggerFactory.getLogger(Block.class);
	public String transactionId; //Contains a hash of transaction*
        public String sequence; 
        public String timestamps;
	public transient PublicKey senderPubKey; //Senders address public key.
        public String sender;
	public transient PublicKey recipientPubKey; //Recipients address/public key.
        public String recipient;
	public String message; //Contains the amount we wish to send to the recipient.
	public transient byte[] signatureByte; //This is to prevent anybody else from spending funds in our wallet.
        public String signature;
	
	
	public Transaction (PublicKey from, PublicKey to, String mesg) {
            this.senderPubKey = from;
            this.recipientPubKey = to;
            this.message = mesg;
            this.recipient = StringUtil.getStringFromKey(recipientPubKey);
            this.sender = StringUtil.getStringFromKey(senderPubKey);
        }
        
        public void DecodeString2Key () {            
            try {
                if (!sender.isEmpty() && !recipient.isEmpty() && !signature.isEmpty()){
                    this.senderPubKey = StringUtil.DecodeString2Key(sender);
                    this.recipientPubKey = StringUtil.DecodeString2Key(recipient);
                    this.signatureByte = StringUtil.decodeSignature(signature);
                }   
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
                LOGGER.error("Error while decoding signature.");
            }      
        }
        
	public boolean processTransaction() {		
            if(verifySignature() == false) {
                LOGGER.info("Transaction Signature failed to verify");
                return false;
            }
            if(verifyCypherText() == false) {
                LOGGER.info("Transaction data was changed.");
                return false;
            }
            return true;
	}
	
	public void generateSignature(PrivateKey privateKey) {  
            try {
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                sequence = String.valueOf(random.nextInt());
            } catch (NoSuchAlgorithmException ex) {
                LOGGER.info("No random generator algo exists.");
            }
            String data = StringUtil.getStringFromKey(senderPubKey) + StringUtil.getStringFromKey(recipientPubKey) + timestamps + message; 
            transactionId = calulateHash(data);
            signatureByte = StringUtil.applyECDSASig(privateKey,transactionId);
            signature = StringUtil.encodeSignature(signatureByte);
	}
	
	public boolean verifySignature() {
            return StringUtil.verifyECDSASig(senderPubKey, transactionId, signatureByte);
	}	
        
        public boolean verifyCypherText() {
            String data = StringUtil.getStringFromKey(senderPubKey) + StringUtil.getStringFromKey(recipientPubKey) + timestamps + message;
            return (calulateHash(data) == null ? transactionId == null : calulateHash(data).equals(transactionId));
        }
	private String calulateHash(String data) {
            return StringUtil.applySha256(data + sequence);
	}
        
        @Override
        public boolean equals (Object o) {
            Transaction tx = (Transaction) o;
            return (tx.transactionId == null ? this.transactionId == null : tx.transactionId.equals(this.transactionId));
        }
}

