package blockchain;
import Utils.StringUtil;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class NodeWallet {
    // not 49byte eckey, rather 75byte key
    public transient ECPrivateKey privateKey;
    public transient ECPublicKey publicKey;
    public String creator;
    public String publickey;
    public String privatekey;

    public NodeWallet() {
        generateKeyPair();
        this.publickey = StringUtil.getStringFromKey(publicKey);
        this.privatekey = StringUtil.getStringFromKey(privateKey);
    }
    
    public NodeWallet(String pub, String priv) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this.publickey = pub;
        this.privatekey = priv;
        unlockWallet();
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
            privateKey = (ECPrivateKey) keyPair.getPrivate();
            publicKey = (ECPublicKey) keyPair.getPublic();

        }catch(InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction sendFunds(PublicKey _recipient, String mesg) {
        Transaction transaction = new Transaction();
        transaction.setInputVariable(publicKey, _recipient, mesg);        
        transaction.generateSignature(privateKey);
        return transaction;
    }

    private void unlockWallet() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA","BC");
        
        byte[] pubEncoded = StringUtil.getKeyByteFromString(publickey);
        byte[] privEncoded = StringUtil.getKeyByteFromString(privatekey);
        X509EncodedKeySpec  x509EncodedKeySpec = new X509EncodedKeySpec (pubEncoded);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privEncoded);
        ECPublicKey publicKeyCheck = (ECPublicKey) ecKeyFac.generatePublic(x509EncodedKeySpec);
        ECPrivateKey privateKeyCheck = (ECPrivateKey)ecKeyFac.generatePrivate(pkcs8EncodedKeySpec);
        
        if (StringUtil.verifyECDSASig(publicKeyCheck, "helloworld", StringUtil.applyECDSASig(privateKeyCheck,"helloworld"))){
            this.publicKey = publicKeyCheck;
            this.privateKey = privateKeyCheck;
        } 
    }
}


