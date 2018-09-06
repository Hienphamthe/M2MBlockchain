/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import blockchain.Block;
import blockchain.NodeMain;
import blockchain.NodeWallet;
import blockchain.Transaction;
import com.google.gson.Gson; 
import com.google.gson.GsonBuilder; 
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.json.simple.parser.ParseException;

public class testcase {
    public static void main(String[] args) {
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//        NodeWallet test = new NodeWallet();
//        ECPublicKey publicKey = (ECPublicKey) test.publicKey;
//        System.out.println(encodeECPublicKey(publicKey).length);
//        System.out.println(test.publicKey.getEncoded().length);
//                                    for(Object x : NodeMain.TxMap) {
//                                    Class<?> clazz = x.getClass();
//                                    Field field;
//                                    try {
//                                        field = clazz.getField("transactionId"); //Note, this can throw an exception if the field doesn't exist.
//                                        Object fieldValue = field.get(x);
//                                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
//                                        java.util.logging.Logger.getLogger(NodeMain.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
//                                    
//                                    List<Transaction> resultTx = NodeMain.TxMap.stream()
//                                    .filter(a -> Objects.equals(a.getHash(), "303379aeb5006bda33d477a512a214737befbd369b64e1566f41f519d5ccf38d"))
//                                    .collect(Collectors.toList());  
//                                }
    }
//    public static void main(String[] args) throws ParseException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException { 
//        final Gson gson = new GsonBuilder().create();
//        final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();  
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//
//        
//        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");        
//        keyGen.initialize(new ECGenParameterSpec("prime192v1"), SecureRandom.getInstance("SHA1PRNG")); //256 
//        KeyPair keyPair = keyGen.generateKeyPair();
//        KeyPair keyPair2 = keyGen.generateKeyPair();
//
//        PrivateKey privateKey = keyPair.getPrivate();       
//        PublicKey publicKey = keyPair.getPublic();              
//        PublicKey publicKey2 = keyPair2.getPublic();        
//        
//        TransactionTest genesisTransaction = new TransactionTest();
//        genesisTransaction.setInputVariable(publicKey, publicKey2, "service1_good");    
//        genesisTransaction.generateSignature(privateKey);
//        genesisTransaction.transactionId = "0";
//        String json = gson.toJson(genesisTransaction);
//        System.out.println(prettyGson.toJson(json));
//        
//        
//        
//        //// recieved side
//        
//        
//        
//        
//        TransactionTest receivedTransaction = gson.fromJson(json, TransactionTest.class);        
//        // now take the encoded value and recreate the private key
//        byte[] senderEncoded = StringUtil.getKeyByteFromString(receivedTransaction.senderBase58);
//        byte[] reciepientEncoded = StringUtil.getKeyByteFromString(receivedTransaction.reciepientBase58);
//        KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA","BC");
//        X509EncodedKeySpec  pkcs8EncodedKeySpecSender = new X509EncodedKeySpec (senderEncoded);
//        X509EncodedKeySpec  pkcs8EncodedKeySpecReciepient = new X509EncodedKeySpec (reciepientEncoded);        
//        PublicKey sender = ecKeyFac.generatePublic(pkcs8EncodedKeySpecSender);
//        PublicKey reciepient = ecKeyFac.generatePublic(pkcs8EncodedKeySpecReciepient);
//        receivedTransaction.setDecodedKey(sender, reciepient);
//        if (receivedTransaction.verifySignature()) {
//            System.out.println("verified");
//        } else {
//            System.out.println("wrong");
//        }
        
        
        
//        String mesg = "abc";
//        Transaction tx = walletA.sendFunds(walletB.publicKey, mesg);
//        String json = gson.toJson(tx);
//        
//        
//        JSONParser parser = new JSONParser();
//        JSONObject base = (JSONObject) parser.parse(json);
//        //Transaction base = gson.fromJson(json, Transaction.class); 
//        //if("BaseRevised".equalsIgnoreCase(base.)){System.out.println(prettyGson.toJson(base));};
//        System.out.println(prettyGson.toJson(base));
//        
//        Transaction receivedTx = new Transaction();
//        //receivedTx.setInputVariable(base.get("sender"), base.get("reciepient"), base.get("message"));
//    } 
        public static byte[] encodeECPublicKey(ECPublicKey pubKey) {
        int keyLengthBytes = pubKey.getParams().getOrder().bitLength() / 8;
        byte[] publicKeyEncoded = new byte[1 + 2 * keyLengthBytes];

        publicKeyEncoded[0] = 0x04;

        // You probably know a better way to copy bytes...
        BigInteger x = pubKey.getW().getAffineX();
        byte[] xba = x.toByteArray();
        for (int i = 0; i < keyLengthBytes; i++) {
            publicKeyEncoded[1 + i] = xba[i];
        }

        BigInteger y = pubKey.getW().getAffineY();
        byte[] yba = y.toByteArray();
        for (int i = 0; i < keyLengthBytes; i++) {
            publicKeyEncoded[1 + keyLengthBytes + i] = yba[i];
        }

        return publicKeyEncoded;
    }
}
