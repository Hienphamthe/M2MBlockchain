package Utils;

import blockchain.Transaction;
import java.security.*;
import java.util.ArrayList;
import com.google.gson.GsonBuilder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List; 
import java.util.Set;

public class StringUtil {
    private NetworkInterface ni;
    
    //For linux, get ip address from interface
    public String getIP(String adapter) {
        //Only works on LINUX-Based systems... windows needs to be added
        try {
            ni = NetworkInterface.getByName(adapter);
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();
                if (!ia.isLinkLocalAddress()) {
                    return ia.getHostAddress().toString();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    //Check is local ip address
    public static boolean isLocal(String host) {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();

                //Remove loopback interface/subinterface/not running interface
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null) {
                        if (ip.getHostAddress().equals(host))                        
                            return true;
                    }
                }
            }
        } catch (SocketException e) {
                e.printStackTrace();
        }
        return false;
    }
    
    //Applies Sha256 to a string and returns the result. 
    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            //Applies sha256 to our input, 
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuffer hexString = new StringBuffer(); // This will contain hash as hexidecimal
            for (int i = 0; i < hash.length; i++) {
                    String hex = Integer.toHexString(0xff & hash[i]);
                    if(hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Applies ECDSA Signature and returns the result ( as bytes ).
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    //Verifies a String signature 
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Short hand helper to turn Object into a json string
    public static String getJson(Object o) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    //Returns difficulty string target, to compare to hash. eg difficulty of 5 will return "00000"  
    public static String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    public static String getStringFromKey(Key key) {
        return Base58.encode(key.getEncoded());
    }
    
    public static byte[] getKeyByteFromString (String keyString) {
        return Base58.decode(keyString);
    }
    
    public static String encodeSignature (byte[] sig) {
        return Base58.encode(sig);
    }
    
    public static byte[] decodeSignature (String sigString) {
        return Base58.decode(sigString);
    }

    
    public static String getMerkleRoot(List<Transaction> transactions) {
        ArrayList<String> tree = new ArrayList<>();
        for (Transaction t : transactions) {
            tree.add(t.transactionId);
        }
        int levelOffset = 0;
        for (int levelSize = transactions.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                String tleft = tree.get(levelOffset + left);
                String tright = tree.get(levelOffset + right);
                tree.add(applySha256(tleft + tright));
            }
            levelOffset += levelSize;
        }
        return tree.get(tree.size()-1);
    }
    
    // Check a string is blank
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    // Check a string is not blank
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }   
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> removeDuplicate(List <T> list) {
        Set <T> set = new HashSet <>();
        List <T> newList = new ArrayList <>();
        list.stream().filter((element) -> (set.add((T) element))).forEachOrdered((element) -> {
            newList.add((T) element);
            });
        list.clear();
        list.addAll(newList);
        return list;
    }
}