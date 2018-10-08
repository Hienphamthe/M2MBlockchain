package blockchain;
import Utils.StringUtil;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
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
            String data = StringUtil.getStringFromKey(senderPubKey) + StringUtil.getStringFromKey(recipientPubKey) + timestamps + message; 
            try {
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                sequence = String.valueOf(random.nextInt());
            } catch (NoSuchAlgorithmException ex) {
                LOGGER.info("No random generator algo exists.");
            }
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
        
        public void setInputVariable (PublicKey from, PublicKey to, String mesg) {
            this.senderPubKey = from;
            this.recipientPubKey = to;
            this.message = mesg;
            this.recipient = StringUtil.getStringFromKey(recipientPubKey);
            this.sender = StringUtil.getStringFromKey(senderPubKey);
        }

        @Override
        public boolean equals (Object o) {
            Transaction tx = (Transaction) o;
            return (tx.transactionId == null ? this.transactionId == null : tx.transactionId.equals(this.transactionId));
        }
        
        public void Transaction () {            
            try {
                if (!sender.isEmpty() && !recipient.isEmpty() && !signature.isEmpty()){
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                byte[] senderEncoded = StringUtil.getKeyByteFromString(sender);
                byte[] reciepientEncoded = StringUtil.getKeyByteFromString(recipient);
                KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA","BC");
                X509EncodedKeySpec  x509EncodedKeySpecSender = new X509EncodedKeySpec (senderEncoded);
                X509EncodedKeySpec  x509EncodedKeySpecRecipient = new X509EncodedKeySpec (reciepientEncoded);        
                this.senderPubKey = ecKeyFac.generatePublic(x509EncodedKeySpecSender);
                this.recipientPubKey = ecKeyFac.generatePublic(x509EncodedKeySpecRecipient);
                this.signatureByte = StringUtil.decodeSignature(signature);
                }   
                
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
                LOGGER.error("Error while decoding signature.");
            }      
        }
}

