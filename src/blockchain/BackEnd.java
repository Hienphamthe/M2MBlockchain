package blockchain;

import Utils.StringUtil;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    // Create a local blockchain
    public static List<Block> blockChain = new ArrayList<Block>();
    // Create genesis transaction for genesis block 
    private Transaction genesisTransaction;
    // Miscellaneous variables 
    public static String localHost;
    public static int localPort = 8019;
    private static String localSocketDirectory;
    // Read list of known peers (List of IP addresses)
    private final File peerFile = new File("./peers.list");
    // File to store the blockchain database
    private File dataFile;
    // Store wallet addresses
    private NodeWallet myNode = null;    
    private final File addressFile = new File("./addresses.list");
    // Miscellaneous
    private List<String> miningDuty = new ArrayList<>();
    private List<String> fixedMiningDuty;
    private final boolean isAutominingActivate = false; // Only works with VM
    public int bestHeight;
    private ArrayList<String> peersListMainThread;
    public  List<Transaction> TXmempool = new ArrayList<>(); 
    public  List<Transaction> TxMap = new ArrayList<>(); 
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();     
    // </editor-fold>

    public boolean startBackend() throws IOException, InterruptedException{
        peersListMainThread = new ArrayList<>();
        
        // <editor-fold defaultstate="collapsed" desc="Acquire local IP address">
        if (isAutominingActivate) {
            localHost = new StringUtil().getIP("enp0s3");
        } else {
            String[] host = InetAddress.getLocalHost().toString().split("/");
        localHost = host[1];
        }
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
        miningDuty.add(String.valueOf(localPort)); // linux VM
        
        dataFile = new File("./"+localSocketDirectory+"/blockchain.bin");

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());             
        // no previous blockchain currently exists and could not find bootstrap node, create genesis block 
        if (!peerFile.exists() && !dataFile.exists()) {
            createNodeWallet();
            Block genesisBlock = (myNode!=null) ? GenesisBlockGenerator() : null;
            FileUtils.writeStringToFile(dataFile,gson.toJson(genesisBlock), StandardCharsets.UTF_8,true);
            LOGGER.info("Empty peerFile. Creating new one ...");                
            FileUtils.writeStringToFile(peerFile, localHost+":"+localPort,StandardCharsets.UTF_8,true);
        } else {
            if (peerFile.exists()){
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
                if (!isLocalSocket) FileUtils.writeStringToFile(peerFile, "\r\n"+localSocket,StandardCharsets.UTF_8,true);
            }
            if (dataFile.exists()){ 
                LOGGER.info("DataFile exists. Acquiring local local blockchain database ...");
                for(String line:FileUtils.readLines(dataFile, StandardCharsets.UTF_8)){
                    blockChain.add(gson.fromJson(line, Block.class));
                    TxMap.addAll(gson.fromJson(line, Block.class).transactions);
                }
            }
        }
        // </editor-fold>
               
        
//        //(DEBUG) Show current blockchain 
//        LOGGER.info(prettyGson.toJson(blockChain));
//        //(DEBUG) Show current blockheight
//        LOGGER.info("Local blockchain block height: "+bestHeight);
 

        // <editor-fold defaultstate="collapsed" desc="P2p communication">
        /**
         * P2p communication
         * Execute each 2000ms period
         */

        
        TimeUnit.MILLISECONDS.sleep(500);
        long startTime = System.currentTimeMillis();
        int toBlockNum =0;
        LOGGER.info("P2P communication!\n");
        // ********************************
        // Broadcast asking neighbors for genesis block, broadcast duty task
        // ********************************         
        if (blockChain.isEmpty()) peerNetwork.broadcast("VERSION "+0);
        else peerNetwork.broadcast("VERSION "+ blockChain.size());
        if (!miningDuty.isEmpty()) peerNetwork.broadcast("PING "+ gson.toJson(miningDuty));
        
        // <editor-fold defaultstate="collapsed" desc="Handling P2p communication">
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
                            bestHeight = Integer.parseInt(payload);
                            String response = "VERACK " + blockChain.size() + " " + blockChain.get(blockChain.size() - 1).getHash();
                            LOGGER.info("=> [p2p] RESPOND: " + response);
                            pt.peerWriter.write(response);
                        }
                        else if ("VERACK".equalsIgnoreCase(cmd)) {
                            String[] parts = payload.split(" ");
                            bestHeight = Integer.parseInt(parts[0]);
                        } else if ("PING".equalsIgnoreCase(cmd)) {
                            List<String> neighborDutyList = gson.fromJson(payload, new TypeToken<List<String>>(){}.getType());
                            miningDuty.addAll(neighborDutyList);
                            miningDuty = new ArrayList<>(new HashSet<>(miningDuty));
                            String response = "PONG "+ gson.toJson(miningDuty);
                            LOGGER.info("=> [p2p] RESPOND: " + response);
                            pt.peerWriter.write(response);
                        } else if ("PONG".equalsIgnoreCase(cmd)) {
                            List<String> neighborDutyList = gson.fromJson(payload, new TypeToken<List<String>>(){}.getType());
                            miningDuty.addAll(neighborDutyList);
                            miningDuty = new ArrayList<>(new HashSet<>(miningDuty));
                        } else if ("GET_BLOCK".equalsIgnoreCase(cmd)) {
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
                                    // Remove already mined transaction
                                    TXmempool = TXmempool.stream()
                                            .filter(singleTx -> !newBlock.transactions.stream().anyMatch(singleTx::equals))
                                            .collect(Collectors.toList());
                                    if (newBlock.getIndex()==0){FileUtils.writeStringToFile(dataFile,gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
                                    else {FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
                                    peerNetwork.broadcast("BLOCK " + payload);
                                } else if (!(newBlock.getIndex()==0)) {
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
                            if (newTransaction!=null 
                                    && newTransaction.processTransaction() 
                                    && !(TXmempool.stream().anyMatch(newTransaction::equals))
                                    && !(TxMap.stream().anyMatch(newTransaction::equals))){
                                TXmempool.add(newTransaction);                                
                                LOGGER.info("Added transaction " + newTransaction.transactionId + " to mempool.");
                                peerNetwork.broadcast("TRANSACTION " + payload);
                            }                                                                                        
                        }
                    }
                }        
            }
            // </editor-fold>
            
            // <editor-fold defaultstate="collapsed" desc="Automation mining">
            // renew network list every 5sec
            if ((System.currentTimeMillis()-startTime)/1000>10) {
                    List<PeerThread> refresedList = peerNetwork.peerThreads.stream().filter(a -> a.peerReader.isConnected).collect(Collectors.toList());
                    refresedList.forEach((temp)->{System.out.println(temp.getClientSocket());});
                    refresedList.forEach((temp)->{miningDuty.add(temp.getClientSocket().g);});
                    String pingpong = "PING "+ gson.toJson(miningDuty);
                    peerNetwork.broadcast(pingpong);
                    startTime = System.currentTimeMillis();
            }
            
            if (isAutominingActivate){
                if ((System.currentTimeMillis()-startTime)/1000>10) {
                    List<PeerThread> refresedList = peerNetwork.peerThreads.stream().filter(a -> a.peerReader.isConnected).collect(Collectors.toList());
                    refresedList.forEach((temp)->{System.out.println(temp.getClientSocket());});
                   
                    refresedList.forEach((temp)->{miningDuty.add(temp.getClientSocket().getInetAddress().toString().replaceAll("/", ""));});
                    miningDuty = new ArrayList<>(new HashSet<>(miningDuty));
                    String pingpong = "PING "+ gson.toJson(miningDuty);
                    peerNetwork.broadcast(pingpong);
                    startTime = System.currentTimeMillis();
                }

                if (TXmempool.size()>=3){
                    fixedMiningDuty = miningDuty;
                    toBlockNum = (toBlockNum == 0) ? blockChain.size()+fixedMiningDuty.size():toBlockNum;
                    for(int i=blockChain.size(); i<toBlockNum; i++){
                        if (fixedMiningDuty.get(i).equalsIgnoreCase(localHost)){
                            LOGGER.info("Auto mining with difficulty = 3");
                            String logger = MineBlock(3, peerNetwork) ? "Block write Success!" : "Invalid block.";
                            LOGGER.info(logger);
                        }
                        toBlockNum = (i+1==toBlockNum) ? 0 : toBlockNum;
                    }
                }
            }
            
            // </editor-fold>
            
            // <editor-fold defaultstate="collapsed" desc="Compare block height, sync block">
            int localHeight = blockChain.size();
            if (bestHeight > localHeight) {
                LOGGER.info("Local chain height: " + localHeight+" Best chain Height: " + bestHeight);
                TimeUnit.MILLISECONDS.sleep(300);
                for (int i = localHeight; i < bestHeight; i++) {
                    LOGGER.info("=> [p2p] COMMAND: " + "GET_BLOCK " + i);
                    peerNetwork.broadcast("GET_BLOCK " + i);
                }
            }
            // </editor-fold>
            
            // <editor-fold defaultstate="collapsed" desc="Handling RPC communication">
            for (RpcThread th:rpcAgent.rpcThreads) {
                String request = th.req;
                if (request != null) { //any request have 3 parts: verb & indirectobject & directobject
                    String[] parts = request.split(" ");
                    parts[0] = parts[0].toLowerCase();
                    if (null == parts[0]) {
                        th.res = "Unknown command: \"" + parts[0] + "\" ";
                    } else switch (parts[0]) {
                        case "getinfo":
                            th.res = (blockChain.isEmpty()) ? "Empty blockchain." : prettyGson.toJson(blockChain);
                            break;
                        case "createwallet":
                            th.res =(createNodeWallet()) ? "My public key:" +myNode.publickey : "Unable to create new wallet.";
                            break;
                        case "getaddr":
                            th.res = (myNode==null) ? "This node does not control any wallet. Please createwallet or unlock an existing one!" : myNode.publickey;
                            break;
                        case "getpendingtx":
                            th.res = (TXmempool.isEmpty()) ?  "Tx mempool is empty." : prettyGson.toJson(TXmempool);
                            break;
                        case "unlock":
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
                            }   break;
                        case "send":
                            if (parts.length==3){
                                if (myNode==null) {
                                    th.res = "This node does not control any wallet. Please createwallet or unlock an existing one!";
                                } else {
                                    try {
                                        String toAddress = parts[1];
                                        String mesg = parts[2];
                                        if (mesg.split(":").length!=2) {
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
                            break;
                        case "filterblock":
                            if (parts.length==3){
                                List<Block> resultBlock = FilterBlock(parts[1], parts[2]);
                                th.res = resultBlock!=null ? prettyGson.toJson(resultBlock): "Block not found.";
                            } else {
                                th.res = "Wrong syntax.";
                            }   break;
                        case "filtertx":
                            if (parts.length==3){
                                List<Transaction> resultTx = FilterTx(parts[1], parts[2]);
                                th.res = resultTx!=null ? prettyGson.toJson(resultTx):"Transaction not found.";
                            } else {
                                th.res = "Wrong syntax.";
                            }   break;
                        case "mine":
                            if (parts.length==2){
                                try {
                                    int difficulty = Integer.parseInt(parts[1]);
                                    th.res = (TXmempool.isEmpty()) ? 
                                            "Block write failed! No Transaction existed!" :
                                            MineBlock(difficulty, peerNetwork) ? "Block write Success!" : "Invalid block";
                                } catch (NumberFormatException e) {
                                    th.res = "Syntax (no '<' or '>'): mine <difficulty> - mine a block with <difficulty>";
                                }
                            } else {
                                th.res = "Wrong syntax.";
                            }   break;
                        default:
                            th.res = "Unknown command: \"" + parts[0] + "\" ";
                            break;
                    }
                }
            }        
        // </editor-fold>
        TimeUnit.MILLISECONDS.sleep(500);
        } 
        // </editor-fold>
    }
    
    // <editor-fold defaultstate="collapsed" desc="Methods">
    private Block GenesisBlockGenerator () {      
        NodeWallet coinbase = new NodeWallet();
        // hardcode genesisBlock
        // create genesis transaction, which sends message "service1_good" to walletA: 
        genesisTransaction = new Transaction();
        genesisTransaction.setInputVariable(coinbase.publicKey, myNode.publicKey, "genesis:data"); 
        genesisTransaction.timestamps = "2018-07-08 00:00:00";
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
    
    private <T> String FindField(T singleElement, String fieldName) { // T: Block or Tx
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

    private boolean MineBlock(int difficulty, PeerNetwork peerNetwork) {
        boolean status = false;
        Block newBlock = Block.generateBlock(blockChain.get(blockChain.size() - 1), difficulty, TXmempool, localSocketDirectory);
        if (Block.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
            try {
                blockChain.add(newBlock);
                status = true;
                FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);
                peerNetwork.broadcast("BLOCK " + gson.toJson(newBlock));
                TxMap.addAll(TXmempool);                                        
                TXmempool.clear();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(BackEnd.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            status = false;
        }
        return status;
    }
    
    private List<Transaction> FilterTx(String fieldName, String value) {
        List<Transaction> resultTx = TxMap.stream()
            .filter(singleTx -> Objects.equals(FindField(singleTx, fieldName), value))
            .collect(Collectors.toList());
        if (!(resultTx.isEmpty())) {
            return resultTx;
        } else {
            resultTx = TxMap.stream()
                .filter(singleTx -> Objects.equals((singleTx.message.split(":"))[0], value))
                .collect(Collectors.toList());
            if (!(resultTx.isEmpty())) {
                return resultTx;
            } else {
                resultTx = TxMap.stream()
                .filter(singleTx -> Objects.equals((singleTx.message.split(":"))[1], value))
                .collect(Collectors.toList());
                return (!(resultTx.isEmpty())) ? resultTx : null;
            }
        }
    }

    private List<Block> FilterBlock(String fieldName, String value) {
        List<Block> resultBlock = blockChain.stream()
            .filter(singleBlock -> Objects.equals(FindField(singleBlock, fieldName), value))
            .collect(Collectors.toList());
        if (!(resultBlock.isEmpty())) {
            return resultBlock;
        } else {
            List<Transaction> resultTx = FilterTx(fieldName, value);
            if (resultTx==null) {
                return null;
            } else {
                resultBlock = blockChain.stream()
                    .filter(singleBlock ->  {
                        boolean result = false;
                        for (Transaction temp : singleBlock.transactions) {
                            for (Transaction temp2 : resultTx) {
                                if (temp.equals(temp2)) {
                                   result = true;
                                   break;
                                }
                            }
                        }
                        return result;
                    }).collect(Collectors.toList());
                return !resultBlock.isEmpty() ? resultBlock : null;
            }
        }
    }
    
    // </editor-fold>
}
