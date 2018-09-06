package blockchain;
import Utils.StringUtil;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Transaction {
	public String transactionId; //Contains a hash of transaction*
	public transient PublicKey senderPubKey; //Senders address public key.
        public String sender;
	public transient PublicKey recipientPubKey; //Recipients address/public key.
        public String recipient;
	public String message; //Contains the amount we wish to send to the recipient.
	public transient byte[] signatureByte; //This is to prevent anybody else from spending funds in our wallet.
        public String signature;
	
	private static int sequence = 0; //A rough count of how many transactions have been generated 
	
	public boolean processTransaction() {		
            if(verifySignature() == false) {
                System.out.println("Transaction Signature failed to verify");
                return false;
            }								
            return true;
	}
	
	public void generateSignature(PrivateKey privateKey) {            
		String data = StringUtil.getStringFromKey(senderPubKey) + StringUtil.getStringFromKey(recipientPubKey) + message;                
		signatureByte = StringUtil.applyECDSASig(privateKey,data);
                signature = StringUtil.encodeSignature(signatureByte);
                transactionId = calulateHash();
	}
	
	public boolean verifySignature() {
		String data = StringUtil.getStringFromKey(senderPubKey) + StringUtil.getStringFromKey(recipientPubKey) + message;                
		return StringUtil.verifyECDSASig(senderPubKey, data, signatureByte);
	}	
	
	private String calulateHash() {
		sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
		return StringUtil.applySha256(StringUtil.getStringFromKey(senderPubKey) +
				StringUtil.getStringFromKey(recipientPubKey) +
				message + sequence
				);
	}
        
        public void setInputVariable (PublicKey from, PublicKey to, String mesg) {
            this.senderPubKey = from;
            this.recipientPubKey = to;
            this.message = mesg;
            this.recipient = StringUtil.getStringFromKey(recipientPubKey);
            this.sender = StringUtil.getStringFromKey(senderPubKey);
        }
        
        public boolean isMine(PublicKey publicKey) {
            return (publicKey == recipientPubKey);
	}
        
        public void Transaction () throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {            
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
        }
}
