package blockchain;

import MiningThread.MiningThread;
import REST.StartRestServer;
import REST.StartRestClient;
import Utils.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p2p.PeerNetwork;
import p2p.RESTClientNetwork;
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
    public static int localPort = 8017;
    private static String localSocketDirectory;
    // Read list of known peers (List of IP addresses)
    private File peerFile;
    // File to store the blockchain database
    private File dataFile;
    // Store wallet addresses
    private NodeWallet myNode = null;    
    private File addressFile;
    // Miscellaneous
    private List<String> miningDuty = new ArrayList<>();
    private List<String> fixedMiningDuty = new ArrayList<>();
    private boolean isAutoSelectiveMiningActivate = false; // Only works with VM
    private boolean isAutoMiningActivate = false; // Only works with VM
    public int bestHeight;
    private ArrayList<String> peersListMainThread;
    public  List<Transaction> TXmempool = new ArrayList<>(); 
    public  List<Transaction> TxMap = new ArrayList<>(); 
    public Thread mineT;
    public MiningThread mineObj;
    public boolean runningMiningThread = false;
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    // Extension
    private File weightFile;
    private boolean isWeightConcept = false; // Only works with VM
    private List<String> weightMiningList = new ArrayList<>();
    // </editor-fold>

    public void startBackend() throws IOException, InterruptedException{
        peersListMainThread = new ArrayList<>();
        
        // <editor-fold defaultstate="collapsed" desc="Acquire local IP address">
        if (isAutoSelectiveMiningActivate || isAutoMiningActivate || isWeightConcept) {
            localHost = new StringUtil().getIP("enp0s3");
        } else {
            String[] host = InetAddress.getLocalHost().toString().split("/");
            localHost = host[1];
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Accepting other nodes && start RPC service">
//        PeerNetwork peerNetwork = new PeerNetwork(localPort);
//        peerNetwork.start();
        //Start rest client network
        RESTClientNetwork restClientNetwork = new RESTClientNetwork();
        new StartRestServer().start(localHost, localPort);
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
        addressFile = new File("./addresses.list");
        peerFile = new File("./peers.list");
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
                        continue;
                    }
                    peersListMainThread.add(peer);
                    restClientNetwork.addPeer(addr[0], addr[1]);
//                    restClientNetwork.intro(localSocketDirectory);
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

        // <editor-fold defaultstate="collapsed" desc="P2p communication">
        /**
         * P2p communication
         * Execute each 500ms period
         */

        
        TimeUnit.MILLISECONDS.sleep(500);
        long startTime = System.currentTimeMillis();
        int target = 0;

        LOGGER.info("P2P communication!\n");
        // ********************************
        // Broadcast asking neighbors for genesis block, broadcast duty task
        // ********************************         
        if (blockChain.isEmpty()) restClientNetwork.broadcastGET("block/index/"+0);//restClientNetwork.broadcastPOSTBlockHeight(0);
//        else restClientNetwork.broadcastPOSTBlockHeight(blockChain.size());
        
        
        while (true) {
            // <editor-fold defaultstate="collapsed" desc="Handling P2p communication">
            // Write a file to the newly connected peer, and start the direct connection next time.
//            for (String peer : peerNetwork.peersList) {
//                if (!peersListMainThread.contains(peer)) {
//                    peersListMainThread.add(peer);
//                    LOGGER.info("Add new peer to peerFile: "+peer);
//                    FileUtils.writeStringToFile(peerFile, "\r\n"+peer,StandardCharsets.UTF_8,true);                       
//                }
//            }
//            peerNetwork.peersList.clear();

            // Processing communication
//            for (PeerThread pt : peerNetwork.peerThreads) {
//                if (pt == null || pt.peerReader == null) {
//                    break;
//                }
//                List<String> dataList = pt.peerReader.readData();
//                if (dataList == null) {
//                    LOGGER.info("Null return, retry.");
//                    System.exit(-5);
//                    break;
//                }
//
//                for (String data:dataList) {
//                    LOGGER.info("<= [p2p] COMMAND: " + data);
//                    int flag = data.indexOf(' ');
//                    String cmd = flag >= 0 ? data.substring(0, flag) : data;
//                    String payload = flag >= 0 ? data.substring(flag + 1) : "";
//                    if (StringUtil.isNotBlank(cmd)) {
//                        if ("VERSION".equalsIgnoreCase(cmd)) {
//                            bestHeight = Integer.parseInt(payload);
//                            String response = "VERACK " + blockChain.size() + " " + blockChain.get(blockChain.size() - 1).getHash();
//                            LOGGER.info("=> [p2p] RESPOND: " + response);
//                            pt.peerWriter.write(response);
//                        }
//                        else if ("VERACK".equalsIgnoreCase(cmd)) {
//                            String[] parts = payload.split(" ");
//                            bestHeight = Integer.parseInt(parts[0]);
//                        } else if ("GET_BLOCK".equalsIgnoreCase(cmd)) {
//                            Block block = blockChain.get(Integer.parseInt(payload));
//                            if (block != null) {
//                                String response = "BLOCK " + gson.toJson(block);
//                                LOGGER.info("=> [p2p] RESPOND: "+response);
//                                pt.peerWriter.write(response);
//                            }
//                        } else if ("BLOCK".equalsIgnoreCase(cmd)) {
//                            //Store the block given by the other party in the chain
//                            if (runningMiningThread){
//                                if (mineT.isAlive()){
//                                    mineObj.suspendRequest();
//                                }   
//                            }          
//                            Block newBlock = gson.fromJson(payload, Block.class);
//                            for (Transaction TxInBlock : newBlock.transactions){
//                                TxInBlock.DecodeString2Key();
//                            }
//                            if (!(blockChain.stream().anyMatch(newBlock::equals))) {
//                                // if dont have any block in current blockchain OR                               
//                                // Check the block, if successful, write it to the local blockchain
//                                if (blockChain.isEmpty() || Block.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
//                                    if (runningMiningThread){
//                                        mineObj.newBlock = null;
//                                        mineT.interrupt();
//                                    }
//                                    blockChain.add(newBlock);
//                                    TxMap.addAll(newBlock.transactions);
//                                    LOGGER.info("Added block " + newBlock.getIndex() + " with hash: ["+ newBlock.getHash() + "]");
//                                    // Remove already mined transaction
//                                    TXmempool = TXmempool.stream()
//                                            .filter(singleTx -> !newBlock.transactions.stream().anyMatch(singleTx::equals))
//                                            .collect(Collectors.toList());
//                                    if (newBlock.getIndex()==0){FileUtils.writeStringToFile(dataFile,gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
//                                    else {FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);}
//                                    peerNetwork.broadcast("BLOCK " + payload);
//                                } else if (newBlock.getIndex()!=0) {
//                                    LOGGER.info("Validating Invalid block.");
//                                    if (runningMiningThread){
//                                        mineObj.resumeRequest();
//                                    }
//                                }
//                            }
//                        }
//                        else if ("TRANSACTION".equalsIgnoreCase(cmd)) {
//                            Transaction newTransaction = gson.fromJson(payload, Transaction.class);
//                            newTransaction.DecodeString2Key();
//                            if (newTransaction!=null 
//                                    && newTransaction.processTransaction() 
//                                    && !(TXmempool.stream().anyMatch(newTransaction::equals))
//                                    && !(TxMap.stream().anyMatch(newTransaction::equals))){
//                                TXmempool.add(newTransaction);                                
//                                LOGGER.info("Added transaction " + newTransaction.transactionId + " to mempool.");
////                                peerNetwork.broadcast("TRANSACTION " + payload);
//                            }                                                                                        
//                        }
//                    }
//                }        
//            }
//            // </editor-fold>            
//            
//            // <editor-fold defaultstate="collapsed" desc="Handling RPC communication">
//            for (RpcThread th:rpcAgent.rpcThreads) {
//                String request = th.req;
//                if (request != null) { //any request have 3 parts: verb & indirectobject & directobject
//                    String[] parts = request.split(" ");
//                    parts[0] = parts[0].toLowerCase();
//                    if (null == parts[0]) {
//                        th.res = "Unknown command: \"" + parts[0] + "\" ";
//                    } else switch (parts[0]) {
//                        case "getinfo":
//                            th.res = (blockChain.isEmpty()) ? "Empty blockchain." : prettyGson.toJson(blockChain);
//                            break;
//                        case "createwallet":
//                            th.res =(createNodeWallet()) ? "My public key:" +myNode.publickey : "Unable to create new wallet.";
//                            break;
//                        case "getaddr":
//                            th.res = (myNode==null) ? "This node does not control any wallet. Please createwallet or unlock an existing one!" : myNode.publickey;
//                            break;
//                        case "getpendingtx":
//                            th.res = (TXmempool.isEmpty()) ?  "Tx mempool is empty." : prettyGson.toJson(TXmempool);
//                            break;
//                        case "unlock":
//                            if (parts.length==3){
//                                if (myNode!=null){
//                                    th.res = "Wallet is existing. Address: " +myNode.publickey;
//                                } else {
//                                    String publicKey = parts[1];
//                                    String privateKey = parts[2];
//                                    myNode = new NodeWallet(publicKey, privateKey);
//                                    if (!myNode.unlockWallet()) {
//                                        th.res = "Wrong authenication!";
//                                        myNode = null;
//                                    } else {
//                                        th.res = "Address :"+myNode.publickey+" is validated!";
//                                    }                                    
//                                }
//                                
//                            } else {
//                                th.res = "Wrong syntax.";
//                            }   break;
//                        case "send":
//                            if (parts.length==3){
//                                if (myNode==null) {
//                                    th.res = "This node does not control any wallet. Please createwallet or unlock an existing one!";
//                                } else {
//                                    try {
//                                        String toAddress = parts[1];
//                                        String mesg = parts[2];
//                                        if (mesg.split(":").length!=2) {
//                                            th.res = "Wrong syntax - send <to address> <data>";
//                                        } else {
//                                            LOGGER.info("My node is Attempting to send message ("+mesg+") to "+toAddress+"...");
//                                            Transaction tx = myNode.sendFunds(StringUtil.DecodeString2Key(toAddress), mesg);
//                                            if(tx!=null && tx.processTransaction()){
//                                                TXmempool.add(tx);
//                                                th.res = "Transaction write Success!";
////                                                peerNetwork.broadcast("TRANSACTION " + gson.toJson(tx));
//                                            }
//                                        }
//                                    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
//                                        ex.getStackTrace();
//                                    }
//                                }
//                            } else {
//                                th.res = "Wrong syntax.";
//                            }                  
//                            break;
//                        case "filterblock":
//                            if (parts.length==3){
//                                List<Block> resultBlock = FilterBlock(parts[1], parts[2]);
//                                th.res = resultBlock!=null ? prettyGson.toJson(resultBlock): "Block not found.";
//                            } else {
//                                th.res = "Wrong syntax.";
//                            }   break;
//                        case "filtertx":
//                            if (parts.length==3){
//                                List<Transaction> resultTx = FilterTx(parts[1], parts[2]);
//                                th.res = resultTx!=null ? prettyGson.toJson(resultTx):"Transaction not found.";
//                            } else {
//                                th.res = "Wrong syntax.";
//                            }   break;
//                        case "mine":
//                            if (isAutoSelectiveMiningActivate) {
//                                th.res = "Autonomous Selective Mining is active! Use command 10 to disable this mode before mine manually.";
//                                break;
//                            }
//                            if (isAutoMiningActivate) {
//                                th.res = "Autonomous Mining is active! Use command 11 to disable this mode before mine manually.";
//                                break;
//                            }
//                            if (parts.length==2){
//                                try {
//                                    int difficulty = Integer.parseInt(parts[1]);
//                                    th.res = (TXmempool.isEmpty()) ? 
//                                            "Block write failed! No Transaction existed!" :
//                                            MineBlock(difficulty, peerNetwork) ? "Block write Success!" : "Invalid block";
//                                } catch (NumberFormatException e) {
//                                    th.res = "Syntax (no '<' or '>'): mine <difficulty> - mine a block with <difficulty>";
//                                }
//                            } else {
//                                th.res = "Wrong syntax.";
//                            }   break;
//                        case "toggle-asm":
//                            isAutoSelectiveMiningActivate = !isAutoSelectiveMiningActivate;
//                            isAutoMiningActivate = isWeightConcept = false;
//                            th.res = "Warning! Only working with fully connected network."
//                                    + "\nWarning! Still under development."
//                                    + "\nDone! isAutoSelectiveMiningActivate status: "+isAutoSelectiveMiningActivate;
//                            break;
//                        case "toggle-am":
//                            isAutoMiningActivate = !isAutoMiningActivate;
//                            isAutoSelectiveMiningActivate = isWeightConcept = false;
//                            th.res = "Done! isAutoMiningActivate status: "+isAutoMiningActivate;
//                            break;
//                        case "toggle-weightmining":
//                            isWeightConcept = !isWeightConcept;
//                            isAutoSelectiveMiningActivate = isAutoMiningActivate = false;
//                            th.res = "Done! isWeightConcept status: "+isWeightConcept;
//                            break;
//                        default:
//                            th.res = "Unknown command: \"" + parts[0] + "\" ";
//                            break;
//                    }
//                }
//            }        
//            // </editor-fold>
//        
//            // <editor-fold defaultstate="collapsed" desc="Automation mining">
//            // renew network list every 10sec
//            int localHeight = blockChain.size();
//            if (isAutoMiningActivate&&(TXmempool.size()>=3)){
//                if (!runningMiningThread) {
//                    LOGGER.info("Auto mining with difficulty = 3");
//                    MineBlock(3);
//                }
//                String logger;
//                if (mineObj.newBlock != null){
//                    try {
//                        blockChain.add(mineObj.newBlock);
//                        FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(mineObj.newBlock), StandardCharsets.UTF_8,true);
////                        peerNetwork.broadcast("BLOCK " + gson.toJson(mineObj.newBlock));
//                        TxMap.addAll(TXmempool);                                        
//                        TXmempool.clear();
//                        runningMiningThread = false;
//                    } catch (IOException ex) {
//                        LOGGER.error("Error while writing to dataFile.");
//                    }
//                    logger = "Block writes successfully!";
//                    LOGGER.info(logger);
//                } 
//            }
//            
//            if (isAutoSelectiveMiningActivate){
//                // refresh mining duty list after 10 seconds
//                if ((System.currentTimeMillis()-startTime)/1000>10) {
//                    try {
//                        List<PeerThread> refreshedList = peerNetwork.peerThreads.stream().filter(a -> a.peerReader.isConnected).collect(Collectors.toList());
//
//                        List<String> refreshedListAddr = new ArrayList<>();
//                        refreshedList.forEach((temp)->{
//                            refreshedListAddr.add(temp.getClientSocket().getInetAddress().toString().replaceAll("/", ""));
//                        });
//                        miningDuty.clear();                        
//                        miningDuty.addAll(refreshedListAddr);
//                        miningDuty.add(String.valueOf(localHost));
//                        miningDuty = IPSort.IPSort(miningDuty);
////                        String pingpong = "PING "+ gson.toJson(miningDuty);
////                        peerNetwork.broadcast(pingpong);
//
//                        startTime = System.currentTimeMillis();
//                    } catch (Exception ex) {
//                        LOGGER.error("Error sorting miningDuty list");
//                        ex.printStackTrace();
//                    }       
//                }
//                if (localHeight!=0) { 
//                    if (TXmempool.size()>=1) { // tx mempool size threshold
//                        if (target==0) {
//                            target = blockChain.size() + miningDuty.size();
//                            for (int i=blockChain.size(); i < target; i++) {
//                                String concat = miningDuty.get(i-blockChain.size()).concat(":"+i);
//                                fixedMiningDuty.add(concat);
//                                System.out.println(concat);
//                            }
//                            System.out.println(fixedMiningDuty);
//                        }
//                        
//                        for (String element : fixedMiningDuty){
//                            String [] parts = element.split(":");
//                            if (parts[0].equalsIgnoreCase(localHost)&&Integer.valueOf(parts[1])==blockChain.size()){
//                                LOGGER.info("Auto selective mining with difficulty = 3");
//                                String logger = MineBlock(3, peerNetwork) ? "Block writes successfully!" : "Invalid block.";
//                                LOGGER.info(logger); 
//                                break;
//                            }    
//                        }
//                        if (blockChain.size()==target){
//                            target =0;
//                            fixedMiningDuty.clear();
//                        }
//                    }
//                }
//            }
//            
//            if (isWeightConcept&&(TXmempool.size()>=1)){
//                while (weightMiningList.isEmpty()) {
//                    createWeightList();
//                }
//                weightMiningList = weightMiningList.stream().filter(x -> {
//                    String[] s = x.split(":");
//                    return (Integer.valueOf(s[1]) >= 50);
//                }).collect(Collectors.toList());
//                int maxWeight = 0;                
//                for (String x: weightMiningList) {
//                    String[] s = x.split(":");
//                    if (Integer.parseInt(s[1]) >= maxWeight) {
//                        maxWeight = Integer.parseInt(s[1]);
//                    }
//                }
//                final int permMaxWeight = maxWeight;
//                
//                String selectedNode = weightMiningList.stream().filter(x -> {
//                    String[] s = x.split(":");
//                    return (Integer.valueOf(s[1]) == permMaxWeight);
//                }).collect(Collectors.toList()).get(0);
//                                                
//                if(selectedNode.split(":")[0].equalsIgnoreCase(localHost) ) {
//                    LOGGER.info("Auto selective mining with difficulty = 3");
//                    String logger = MineBlock(3, peerNetwork) ? "Block writes successfully!" : "Invalid block.";
//                    LOGGER.info(logger); 
//                }
//            }
//            // </editor-fold>
//            
//            // <editor-fold defaultstate="collapsed" desc="Compare block height, sync block">
//            if (bestHeight > localHeight) {
//                LOGGER.info("Local chain height: " + localHeight+" Best chain Height: " + bestHeight);
//                TimeUnit.MILLISECONDS.sleep(300);
//                for (int i = localHeight; i < bestHeight; i++) {
//                    LOGGER.info("=> [p2p] COMMAND: " + "GET_BLOCK " + i);
//                    restClientNetwork.broadcastGET("block/index/"+i);
//                }
//            }
            // </editor-fold>
            
        TimeUnit.MILLISECONDS.sleep(500);
        } 
//         </editor-fold>
    }
    
    // <editor-fold defaultstate="collapsed" desc="Methods">
    private Block GenesisBlockGenerator () {      
        NodeWallet coinbase = new NodeWallet();
        genesisTransaction = new Transaction(coinbase.publicKey, myNode.publicKey, "genesis:data");
        genesisTransaction.timestamps = "2018-07-08 00:00:00";
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
        genesisTransaction.transactionId = "0";                          //manually set the transaction id        
        
        LOGGER.info("Creating and mining Genesis block... ");
        Block genesisBlock = new Block();
        genesisBlock.setPrevHash("0");
        genesisBlock.setIndex(0);
        genesisBlock.setTxAmount(1);
        genesisBlock.setDifficulty(3);
        genesisBlock.setTimestamp("2018-07-08 00:00:00"); // Project starting date
        genesisBlock.addTransaction(genesisTransaction);
        
        // add to TxMap after successfully check Tx
        TxMap.add(genesisTransaction); 
        
        genesisBlock.setCreator(localSocketDirectory);
        genesisBlock.setHash(genesisBlock.mineBlock());
        blockChain.add(genesisBlock);
        return genesisBlock;
    }

    private boolean createNodeWallet() throws IOException {  
        if (!addressFile.exists()) {
            LOGGER.info("Empty addressesFile. Creating new one ..."); 
            newNode();
            FileUtils.writeStringToFile(addressFile,gson.toJson(myNode),StandardCharsets.UTF_8,true);  
            return true;
        } else {
            LOGGER.info("AddressesFile exists.");
            if (!(FileUtils.readLines(addressFile,StandardCharsets.UTF_8)
                    .stream().anyMatch(element -> element.equalsIgnoreCase(localSocketDirectory)))) {
                newNode();
                FileUtils.writeStringToFile(addressFile,"\r\n"+gson.toJson(myNode),StandardCharsets.UTF_8,true); 
                return true;
            }        
        }
        return false;
    }
    private void newNode() {
        myNode = new NodeWallet();
        LOGGER.info("My public key: "+ myNode.publickey);                
        myNode.creator = localSocketDirectory;
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
    
    private void MineBlock(int difficulty) {                
        mineObj = new MiningThread();
        mineObj.setVar(blockChain.get(blockChain.size() - 1), difficulty, TXmempool, localSocketDirectory);
        
        mineT = new Thread(mineObj);
        mineT.start();
        
        runningMiningThread = mineT.isAlive();
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
                LOGGER.error("Error while writing to dataFile.");
            }
        } else {
            status = false;
        }
        return status;
    }
    
    private void ProceedPingPong(String payload) {
        try {
            // handle when node disconnect is not implemented
            List<String> neighborDutyList = gson.fromJson(payload, new TypeToken<List<String>>(){}.getType());
            miningDuty.addAll(neighborDutyList);
            miningDuty = StringUtil.removeDuplicate(miningDuty);
            miningDuty = IPSort.IPSort(miningDuty);            
        } catch (Exception ex) {
            LOGGER.error("Error sorting miningDuty list");
            ex.printStackTrace();
        }
    }   

    private void createWeightList() throws IOException {
        weightFile = new File("./weightFile.list");
        if (!weightFile.exists()){
            FileUtils.writeStringToFile(weightFile,"10.0.0.101"+":20", StandardCharsets.UTF_8,true);
            FileUtils.writeStringToFile(weightFile,"\n10.0.0.102"+":100", StandardCharsets.UTF_8,true);
            FileUtils.writeStringToFile(weightFile,"\n10.0.0.103"+":70", StandardCharsets.UTF_8,true);
            FileUtils.writeStringToFile(weightFile,"\n10.0.0.104"+":40", StandardCharsets.UTF_8,true);
            FileUtils.writeStringToFile(weightFile,"\n10.0.0.105"+":50", StandardCharsets.UTF_8,true);
            for(String line:FileUtils.readLines(weightFile, StandardCharsets.UTF_8)){
                weightMiningList.add(line);
            }
        } else {
        }
    }
    // </editor-fold>
}
