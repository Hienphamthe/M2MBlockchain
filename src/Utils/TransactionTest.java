package Utils;
import java.security.*;

public class TransactionTest {
	
	public String transactionId; //Contains a hash of transaction*
	public transient PublicKey sender; //Senders address public key.
        public String senderBase58;
	public transient PublicKey reciepient; //Recipients address/public key.
        public String reciepientBase58;
	public String message; //Contains the amount we wish to send to the recipient.
	public byte[] signature; //This is to prevent anybody else from spending funds in our wallet.
	
	private static int sequence = 0; //A rough count of how many transactions have been generated 
	
	public boolean processTransaction() {
		
		if(verifySignature() == false) {
			System.out.println("Transaction Signature failed to verify");
			return false;
		}						
		return true;
	}
	
	
	public void generateSignature(PrivateKey privateKey) {            
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + message;                
		signature = StringUtil.applyECDSASig(privateKey,data);	
                transactionId = calulateHash();
	}
	
	public boolean verifySignature() {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + message;                
		return StringUtil.verifyECDSASig(sender, data, signature);
	}
	
	
	private String calulateHash() {
		sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
		return StringUtil.applySha256(StringUtil.getStringFromKey(sender) +
				StringUtil.getStringFromKey(reciepient) +
				message + sequence
				);
	}
        
        public void setInputVariable (PublicKey from, PublicKey to, String mesg) {
            this.sender = from;
            this.reciepient = to;
            this.message = mesg;
            this.reciepientBase58 = StringUtil.getStringFromKey(reciepient); 
            this.senderBase58 = StringUtil.getStringFromKey(sender);
        }
        
        public boolean isMine(PublicKey publicKey) {
            return (publicKey == reciepient);
	}
        
        public void setDecodedKey (PublicKey decodedSender, PublicKey decodedReciepient) {
            this.sender = decodedSender;
            this.reciepient = decodedReciepient;
        }
}
