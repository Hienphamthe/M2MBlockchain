package blockchain;

import Utils.StringUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p2p.PeerNetwork;
import p2p.PeerThread;
import p2p.RpcServer;
import p2p.RpcThread;

/**
 *
 * @author Student
 */
public class BackEnd {
    // <editor-fold defaultstate="collapsed" desc="Declaration">
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeMain.class);
    // Create a local blockchain
    public static List<Block> blockChain = new ArrayList<Block>();
    // Create genesis transaction for genesis block 
    private Transaction genesisTransaction;
    // Miscellaneous variables 
    public static String localHost;
    public static final int localPort = 8015;
    private static String localSocketDirectory;
    // Read list of known peers (List of IP addresses)
    private final File peerFile = new File("./peers.list");
    // File to store the blockchain database
    private File dataFile;
    // Store wallet addresses
    private NodeWallet myNode = null;    
    private final File addressFile = new File ("./addresses.list");
    // Miscellaneous
    public int bestHeight;
    private ArrayList<String> peersListMainThread;
    public  List<Transaction> TXmempool = new ArrayList<>(); 
    public  List<Transaction> TxMap = new ArrayList<>(); 
//    private static Block unpackBlock = new Block();
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();     
    // </editor-fold>

    public void startBackend() throws IOException, InterruptedException{
        peersListMainThread = new ArrayList<>();
        
        // <editor-fold defaultstate="collapsed" desc="Acquire local IP address">
        // Linux getIP
//        localHost = new StringUtil().getIP("enp0s3");
        // Windows getIP
        String[] host = InetAddress.getLocalHost().toString().split("/");
        localHost = host[1];
        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Accepting other nodes && start RPC service">
        PeerNetwork peerNetwork = new PeerNetwork(localPort);
        peerNetwork.start(); 
        RpcServer rpcAgent = new RpcServer(localPort+1);
        rpcAgent.start();
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Check peers.list file. Update current blockchain or create a new one">
        /*
         * Check peers.list file
         * If empty file, create file with local socket
         * If exist, to do (explained below)
        */
        localSocketDirectory = localHost+"@"+localPort;
        dataFile = new File("./"+localSocketDirectory+"/blockchain.bin");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());             
        // no previous blockchain currently exists and could not find bootstrap node, create genesis block 
        if (!peerFile.exists() && !dataFile.exists()) {
            createNodeWallet();
            Block genesisBlock = GenesisBlockGenerator();
            FileUtils.writeStringToFile(dataFile,gson.toJson(genesisBlock), StandardCharsets.UTF_8,true);
            LOGGER.info("Empty peerFile. Creating new one ...");                
            FileUtils.writeStringToFile(peerFile, localHost+":"+localPort,StandardCharsets.UTF_8,true);
        }else if (peerFile.exists() && !dataFile.exists()){
            /*
            2 function: 
            Filter out the local socket on the file to form a peersListMainThread list
            If local socket does not exist in the existing list, add the socket to it
            */
            LOGGER.info("PeerFile exists. Setting connection to each peers on the list ...");
            boolean isLocalSocket = false;
            for (String peer : FileUtils.readLines(peerFile,StandardCharsets.UTF_8)) {
                String[] addr = peer.split(":");
                //Jump to next peer, if the current is local                
                if(StringUtil.isLocal(addr[0])&&String.valueOf(localPort).equals(addr[1])){
                    isLocalSocket = true;
//                    LOGGER.info("1 same host redundancy. Excluded!");
                    continue;
                }
                peersListMainThread.add(peer);
                peerNetwork.connect(addr[0], Integer.parseInt(addr[1]));                
            }
            String localSocket = localHost+":"+localPort;
            if (!isLocalSocket) {FileUtils.writeStringToFile(peerFile, "\r\n"+localSocket,StandardCharsets.UTF_8,true);}
        }
        else if (dataFile.exists()){ // currently have blockchain, read each block to list, also add to txMap list for lookup
            for(String line:FileUtils.readLines(dataFile, StandardCharsets.UTF_8)){
                blockChain.add(gson.fromJson(line, Block.class));
                TxMap.addAll(gson.fromJson(line, Block.class).transactions);
            }
            int initBestHeight = blockChain.size();
            peerNetwork.broadcast("VERSION "+initBestHeight);

        }
        // </editor-fold>
               
        
//        //(DEBUG) Show current blockchain 
//        LOGGER.info(prettyGson.toJson(blockChain));
//        //(DEBUG) Show current blockheight
//        LOGGER.info("Local blockchain block height: "+bestHeight);
 

        // <editor-fold defaultstate="collapsed" desc="P2p communication">
        /**
         * P2p communication
         * Execute each 1000ms period
         */

        
        TimeUnit.SECONDS.sleep(1);
        LOGGER.info("P2P communication!");
        // ********************************
        // Broadcast asking neighbors for genesis block
        // ********************************         
        if (blockChain.isEmpty()) {
            peerNetwork.broadcast("VERSION "+0);
        }
        while (true) {
            // Write a file to the newly connected peer, and start the direct connection next time.
            for (String peer : peerNetwork.peersList) {
                if (!peersListMainThread.contains(peer)) {
                    peersListMainThread.add(peer);
                    LOGGER.info("Add new peer to peerFile: "+peer);
                    FileUtils.writeStringToFile(peerFile, "\r\n"+peer,StandardCharsets.UTF_8,true);                       
                }
            }
            peerNetwork.peersList.clear();

            // Processing communication
            for (PeerThread pt : peerNetwork.peerThreads) {
                if (pt == null || pt.peerReader == null) {
                    break;
                }
                List<String> dataList = pt.peerReader.readData();
                if (dataList == null) {
                    LOGGER.info("Null return, retry.");
                    System.exit(-5);
                    break;
                }

                for (String data:dataList) {
                    LOGGER.info("<= [p2p] COMMAND: " + data);
                    int flag = data.indexOf(' ');
                    String cmd = flag >= 0 ? data.substring(0, flag) : data;
                    String payload = flag >= 0 ? data.substring(flag + 1) : "";
                    if (StringUtil.isNotBlank(cmd)) {
                        if ("VERSION".equalsIgnoreCase(cmd)) {
                            String[] parts = payload.split(" ");
                            bestHeight = Integer.parseInt(parts[0]);
                            String response = "VERACK " + blockChain.size() + " " + blockChain.get(blockChain.size() - 1).getHash();
                            LOGGER.info("=> [p2p] RESPOND: " + response);
                            pt.peerWriter.write(response);
                        }
                        else if ("VERACK".equalsIgnoreCase(cmd)) {
                            String[] parts = payload.split(" ");
                            bestHeight = Integer.parseInt(parts[0]);
                        }
                        else if ("GET_BLOCK".equalsIgnoreCase(cmd)) {
                            Block block = blockChain.get(Integer.parseInt(payload));
                            if (block != null) {
                                String response = "BLOCK " + gson.toJson(block);
                                LOGGER.info("=> [p2p] RESPOND: "+response);
                                pt.peerWriter.write(response);
                            }
                        } else if ("BLOCK".equalsIgnoreCase(cmd)) {
                            //Store the block given by the other party in the chain
                            Block newBlock = gson.fromJson(payload, Block.class);                       
                            if (!blockChain.contains(newBlock)) {
                                // if dont have any block in current blockchain OR                               
                                // Check the block, if successful, write it to the local blockchain
                                if (blockChain.isEmpty() || Block.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
                                    blockChain.add(newBlock);
                                    TxMap.addAll(newBlock.transactions);
                                    LOGGER.info("Added block " + newBlock.getIndex() + " with hash: ["+ newBlock.getHash() + "]");
                                    if (newBlock.getIndex()==0){FileUtils.writeStringToFile(dataFile,gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
                                    else {FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
                                    peerNetwork.broadcast("BLOCK " + payload);
                                } else {
                                    LOGGER.info("Invalid block.");
                                }   
                            }
                        }
                        else if ("TRANSACTION".equalsIgnoreCase(cmd)) {
                            Transaction newTransaction = gson.fromJson(payload, Transaction.class);
                            try {
                                newTransaction.Transaction();
                            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
                                ex.printStackTrace();
                            }
                            if (newTransaction!=null && newTransaction.processTransaction()){
                                TXmempool.add(newTransaction);                                
                                LOGGER.info("Added transaction " + newTransaction.transactionId + " to mempool.");
                            }                                                                                        
                        }
                    }
                }                    
            }
            
            // ********************************
            // Compare block height, sync block
            // ********************************
            int localHeight = blockChain.size();
            if (bestHeight > localHeight) {
                LOGGER.info("Local chain height: " + localHeight+" Best chain Height: " + bestHeight);
                TimeUnit.MILLISECONDS.sleep(300);
                for (int i = localHeight; i < bestHeight; i++) {
                    LOGGER.info("=> [p2p] COMMAND: " + "GET_BLOCK " + i);
                    peerNetwork.broadcast("GET_BLOCK " + i);
                }
            }
            
            
            // ********************************
            // Handling RPC services
            // ********************************
            for (RpcThread th:rpcAgent.rpcThreads) {
                String request = th.req;
                if (request != null) { //any request have 3 parts: verb & indirectobject & directobject
                    String[] parts = request.split(" ");
                    parts[0] = parts[0].toLowerCase();
                    if ("getinfo".equals(parts[0])) {
                        th.res = (blockChain.isEmpty()) ? "Empty blockchain." : prettyGson.toJson(blockChain);
                    } else if ("createwallet".equals(parts[0])) {
                        th.res =(createNodeWallet()) ? "My public key:" +myNode.publickey : "Unable to create new wallet.";
                    } else if("getaddr".equals(parts[0])){
                        th.res = (myNode==null) ? "This node does not control any wallet. Please createwallet or unlock an existing one!" : myNode.publickey;
                    } else if("getpendingtx".equals(parts[0])){
                        th.res = (TXmempool.isEmpty()) ?  "Tx mempool is empty." : prettyGson.toJson(TXmempool);                        
                    } else if("unlock".equals(parts[0])){
                        if (parts.length==3){
                            try {
                                String publicKey = parts[1];
                                String privateKey = parts[2];
                                myNode = new NodeWallet(publicKey, privateKey);
                                th.res = (myNode==null) ? "Wrong authenication!" : "Address :"+myNode.publickey+" is validated!";                            
                            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            th.res = "Wrong syntax.";
                        }       
                    } else if("send".equals(parts[0])){
                        if (parts.length==3){
                            if (myNode==null) {
                                th.res = "This node does not control any wallet. Please createwallet or unlock an existing one!";
                            } else {
                                try {
                                    String toAddress = parts[1];
                                    String mesg = parts[2];
                                    if (toAddress.isEmpty() || mesg.isEmpty()) {
                                        th.res = "Wrong syntax - send <to address> <data>";
                                    } else {
                                        LOGGER.info("My node is Attempting to send message ("+mesg+") to "+toAddress+"...");
                                        Transaction tx = myNode.sendFunds(DecodeRecipient(toAddress), mesg);
                                        if(tx!=null && tx.processTransaction()){                                    
                                            TXmempool.add(tx);
                                            th.res = "Transaction write Success!";
                                            peerNetwork.broadcast("TRANSACTION " + gson.toJson(tx));
                                        }
                                    }
                                } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
                                    ex.getStackTrace();
                                }
                            }
                        } else {
                            th.res = "Wrong syntax.";
                        }
//                    } else if("buildreport".equals(parts[0])){
//                        System.out.println("1");
//                        th.res = "Start building report.";
//                        while(!th.req.equalsIgnoreCase("done")){
//                            String[] reportElement = th.req.split(" ");
//                            if (reportElement.length==2){
//                                String serviceName = reportElement[0];
//                                String Comment = reportElement[1];
//                            } else {
//                                th.res = "Wrong report syntax.";
//                                break;
//                            }
//                        }                    
                    } else if ("filterblock".equals(parts[0])) {
                        if (parts.length==3){
                            String fieldName = parts[1];
                            String value = parts[2];
                            List<Block> resultBlock = null;
                            resultBlock = blockChain.stream()
                                    .filter(singleBlock -> Objects.equals(FindField(singleBlock, fieldName), value))
                                    .collect(Collectors.toList());
                            if (!(resultBlock.isEmpty())) {
                                th.res = prettyGson.toJson(resultBlock);
                            } else {
                                resultBlock = blockChain.stream()
                                        .filter(singleBlock -> Objects.equals(FindField(singleBlock.transactions.listIterator(0).next(), fieldName), value))
                                        .collect(Collectors.toList());
                                th.res = (!(resultBlock.isEmpty())) ? prettyGson.toJson(resultBlock) : "No block found.";
                            }
                        } else {
                            th.res = "Wrong syntax.";
                        }
                    }else if ("filtertx".equals(parts[0])) {                        
                        if (parts.length==3){
                            String fieldName = parts[1];
                            String value = parts[2];
                            List<Transaction> resultTx = TxMap.stream()
                                    .filter(singleTx -> Objects.equals(FindField(singleTx, fieldName), value))
                                    .collect(Collectors.toList());
                            th.res = !resultTx.isEmpty() ? prettyGson.toJson(resultTx) : "No transaction found.";
                        } else {
                            th.res = "Wrong syntax.";
                        }             
                    } else if ("mine".equals(parts[0])) {
                        if (parts.length==2){
                            try {
                                int difficulty = Integer.parseInt(parts[1]);
                                // Mining and packing new blocks
                                if(TXmempool.isEmpty()){
                                    th.res = "Block write failed! No Transaction existed!";
                                } else {
                                        Block newBlock = Block.generateBlock(blockChain.get(blockChain.size() - 1), difficulty, TXmempool, localSocketDirectory);
                                        if (Block.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
                                            blockChain.add(newBlock);
                                            th.res = "Block write Success!";
                                            FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);
                                            peerNetwork.broadcast("BLOCK " + gson.toJson(newBlock));
                                            TxMap.addAll(TXmempool);
                                            TXmempool.clear();                                        
                                        } else {
                                            th.res = "Invalid block";
                                        }
                                    }
                            } catch (Exception e) {
                                th.res = "Syntax (no '<' or '>'): send <mesg> - message to send";
                                LOGGER.error("invalid mesg");
                            }
                        } else {
                            th.res = "Wrong syntax.";
                        }
                    }
                    else {
                        th.res = "Unknown command: \"" + parts[0] + "\" ";
                    }
                }
            }
        TimeUnit.SECONDS.sleep(2);
        // </editor-fold>
        } 
    }
    
    private Block GenesisBlockGenerator () {      
        NodeWallet coinbase = new NodeWallet();
        // hardcode genesisBlock
        // create genesis transaction, which sends message "service1_good" to walletA: 
        genesisTransaction = new Transaction();
        genesisTransaction.setInputVariable(coinbase.publicKey, myNode.publicKey, "service1_good");        
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
        genesisTransaction.transactionId = "0";                          //manually set the transaction id        
        
        LOGGER.info("Creating and mining Genesis block... ");
        //Genesis block
        Block genesisBlock = new Block();
        genesisBlock.setPrevHash("0");
        genesisBlock.setIndex(0);
        genesisBlock.setTxAmount(1);
        genesisBlock.setDifficulty(3);
        genesisBlock.setTimestamp("2018-07-08 00:00:00"); // Project starting date
        genesisBlock.addTransaction(genesisTransaction);
        
        // add to TxMap after successfully check Tx
        TxMap.add(genesisTransaction); //its important to store our first transaction in the tx list.
        
        genesisBlock.setCreator(localSocketDirectory);
        genesisBlock.setHash(genesisBlock.mineBlock());
        blockChain.add(genesisBlock);
        return genesisBlock;
    }

    private boolean createNodeWallet() throws IOException {  
        boolean result = false;
        if (!addressFile.exists()) {
            LOGGER.info("Empty addressesFile. Creating new one ..."); 
            newNode();
            FileUtils.writeStringToFile(addressFile,gson.toJson(myNode),StandardCharsets.UTF_8,true);  
            result = true;
        } else {
            LOGGER.info("AddressesFile exists.");
            boolean isLocalSocket = false;
            for (String addr : FileUtils.readLines(addressFile,StandardCharsets.UTF_8)) {
                if (gson.fromJson(addr, NodeWallet.class).creator.equalsIgnoreCase(localSocketDirectory)){
                    isLocalSocket = true;
                    break;                 
                }                                
            }
            if (!isLocalSocket) {
                newNode();
                FileUtils.writeStringToFile(addressFile,"\r\n"+gson.toJson(myNode),StandardCharsets.UTF_8,true); 
                result = true;
            }            
        }
        return result;
    }
    
    private void newNode() {
        myNode = new NodeWallet();
        LOGGER.info("My public key: "+ myNode.publickey);                
        myNode.creator = localSocketDirectory;
    }

    private PublicKey DecodeRecipient(String recipient) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        byte[] reciepientEncoded = StringUtil.getKeyByteFromString(recipient);
        KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA","BC");
        X509EncodedKeySpec  x509EncodedKeySpecRecipient = new X509EncodedKeySpec (reciepientEncoded);   
        return ecKeyFac.generatePublic(x509EncodedKeySpecRecipient);
    }  

    private <T> String FindField(T singleElement, String fieldName) {
        String fieldValue = null;
            Class<?> clazz = singleElement.getClass();
            Field field;
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                fieldValue = field.get(singleElement).toString();
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {                
                LOGGER.error("No such field in FindFieldBlock(). (Optional) Keep searching in transactions field");
            }       
        return fieldValue;
    }
}
