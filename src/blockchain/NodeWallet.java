package blockchain;
import Utils.StringUtil;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeWallet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    // not 49byte eckey, rather 75byte key
    public transient PrivateKey privateKey;
    public transient PublicKey publicKey;
    public String creator;
    public String publickey;
    public String privatekey;

    public NodeWallet() {
        generateKeyPair();
        this.publickey = StringUtil.getStringFromKey(publicKey);
        this.privatekey = StringUtil.getStringFromKey(privateKey);
    }
    
    public NodeWallet(String pub, String priv){
        this.publickey = pub;
        this.privatekey = priv;
    }

    public void generateKeyPair() {
        try {
            // ECDSA: Digital Signature Algorithm (DSA) which uses elliptic curve cryptography. 
            // BC: bouncycastle provider
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC"); // uncompressed: size = 2*(192/8)+1. ECDSA over prime192v1 field
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1"); // 192bit parameter
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random); //256 
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

        }catch(InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction sendFunds(PublicKey _recipient, String mesg) {
        Transaction transaction = new Transaction();
        transaction.setInputVariable(publicKey, _recipient, mesg);
        transaction.timestamps = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        transaction.generateSignature(privateKey);
        return transaction;
    }

    public boolean unlockWallet() {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA","BC");
            
            byte[] pubEncoded = StringUtil.getKeyByteFromString(publickey);
            byte[] privEncoded = StringUtil.getKeyByteFromString(privatekey);
            X509EncodedKeySpec  x509EncodedKeySpec = new X509EncodedKeySpec (pubEncoded);
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privEncoded);
            PublicKey publicKeyCheck = ecKeyFac.generatePublic(x509EncodedKeySpec);
            PrivateKey privateKeyCheck = ecKeyFac.generatePrivate(pkcs8EncodedKeySpec);
            
            if (StringUtil.verifyECDSASig(publicKeyCheck, "helloworld", StringUtil.applyECDSASig(privateKeyCheck,"helloworld"))){                
                this.publicKey = publicKeyCheck;
                this.privateKey = privateKeyCheck; 
                return true;
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
            LOGGER.info("Unlock wallet exception.");
        }
        return false;
    }
}


