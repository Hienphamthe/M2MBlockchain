/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hacker;

import Utils.StringUtil;
import blockchain.Block;
import blockchain.Main;
import blockchain.NodeWallet;
import blockchain.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p2p.PeerNetwork;
import p2p.PeerThread;

/**
 *
 * @author Mrhie
 */
public class HackerBackEnd {
    // <editor-fold defaultstate="collapsed" desc="Declaration">
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    // Create a local blockchain
    public static List<Block> blockChain = new ArrayList<Block>();
    // Create genesis transaction for genesis block 
    private Transaction hackedTx;
    private Block hackedBlock;
    private Transaction genesisTransaction;
    // Miscellaneous variables 
    public static String localHost;
    public static int localPort = 8017;
    private static String localSocketDirectory;
    // Read list of known peers (List of IP addresses)
    private final File peerFile = new File("./peers.list");
    // File to store the blockchain database
    private File dataFile;
    // Store wallet addresses
    private NodeWallet myNode = null;    
    private final File addressFile = new File("./addresses.list");
    // Miscellaneous
    private boolean isAutoSelectiveMiningActivate = false; // Only works with VM
    boolean isAutoMiningActivate = true;
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
        if (isAutoSelectiveMiningActivate || isAutoMiningActivate) {
            localHost = new StringUtil().getIP("enp0s3");
        } else {
            String[] host = InetAddress.getLocalHost().toString().split("/");
            localHost = host[1];
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Accepting other nodes && start RPC service">
        PeerNetwork peerNetwork = new PeerNetwork(localPort);
        peerNetwork.start(); 
        HackerRpcServer rpcAgent = new HackerRpcServer(localPort+1);
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

        // <editor-fold defaultstate="collapsed" desc="P2p communication">
        /**
         * P2p communication
         * Execute each 500ms period
         */

        
        TimeUnit.MILLISECONDS.sleep(500);

        LOGGER.info("P2P communication!\n");
        // ********************************
        // Broadcast asking neighbors for genesis block, broadcast duty task
        // ********************************         
        if (blockChain.isEmpty()) peerNetwork.broadcast("VERSION "+0);
        else peerNetwork.broadcast("VERSION "+ blockChain.size());
        
        
        while (true) {
            // <editor-fold defaultstate="collapsed" desc="Handling P2p communication">
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
                            if (!(blockChain.stream().anyMatch(newBlock::equals))) {
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
                            newTransaction.DecodeString2Key();
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
            
            // <editor-fold defaultstate="collapsed" desc="Handling RPC communication">
            for (HackerRpcThread th:rpcAgent.hackerRpcThreads) {
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
                                if (myNode!=null){
                                    th.res = "Wallet is existing. Address: " +myNode.publickey;
                                } else {
                                    String publicKey = parts[1];
                                    String privateKey = parts[2];
                                    myNode = new NodeWallet(publicKey, privateKey);
                                    if (!myNode.unlockWallet()) {
                                        th.res = "Wrong authenication!";
                                        myNode = null;
                                    } else {
                                        th.res = "Address :"+myNode.publickey+" is validated!";
                                    }                                    
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
                                            hackedTx = myNode.sendFunds(StringUtil.DecodeString2Key(toAddress), mesg);
                                            if(hackedTx!=null && hackedTx.processTransaction()){
                                                TXmempool.add(hackedTx);
                                                th.res = "Transaction write Success!";
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
                        case "maltx_key":
                            Transaction tx = gson.fromJson(prettyGson.toJson(hackedTx), Transaction.class);
                            tx.sender = "24HVuRxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxRoL9Zav";
                            peerNetwork.broadcast("TRANSACTION " + gson.toJson(tx));
                            th.res = prettyGson.toJson(tx);
                            break;
                        case "maltx_message":
                            Transaction tx1 = gson.fromJson(prettyGson.toJson(hackedTx), Transaction.class);
                            tx1.message = "Iam:hacker";
                            peerNetwork.broadcast("TRANSACTION " + gson.toJson(tx1));
                            th.res = prettyGson.toJson(tx1);
                            break;
                        case "mine":
                            if (parts.length==2){
                                try {
                                    int difficulty = Integer.parseInt(parts[1]);
                                    th.res = (TXmempool.isEmpty()) ? 
                                            "Block write failed! No Transaction existed!" :
                                            MineBlock(difficulty) ? "Block write Success!" : "Invalid block";
                                } catch (NumberFormatException e) {
                                    th.res = "Syntax (no '<' or '>'): mine <difficulty> - mine a block with <difficulty>";
                                }
                            } else {
                                th.res = "Wrong syntax.";
                            }   
                            break;   
                        case "malblock_nonce":
                            Block b = gson.fromJson(prettyGson.toJson(hackedBlock), Block.class);
                            b.setNonce(777777);
                            peerNetwork.broadcast("BLOCK " + gson.toJson(b));
                            th.res = prettyGson.toJson(b);
                            break;
                        case "malblock_txmessage":
                            Block b1 = gson.fromJson(prettyGson.toJson(hackedBlock), Block.class);
                            b1.transactions.get(0).message="Iam:hacker";
                            peerNetwork.broadcast("BLOCK " + gson.toJson(b1));
                            th.res = prettyGson.toJson(b1);
                            break;
                        default:
                            th.res = "Unknown command: \"" + parts[0] + "\" ";
                            break;
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
            
        TimeUnit.MILLISECONDS.sleep(500);
        } 
        // </editor-fold>
    }
    
    // <editor-fold defaultstate="collapsed" desc="Methods">
    private Block GenesisBlockGenerator () {      
        NodeWallet coinbase = new NodeWallet();
        // hardcode genesisBlock && create genesis transaction
        genesisTransaction = new Transaction(coinbase.publicKey, myNode.publicKey, "genesis:data");
        genesisTransaction.timestamps = "2018-07-08 00:00:00";
        genesisTransaction.generateSignature(coinbase.privateKey);	 
        genesisTransaction.transactionId = "0";                                
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

    private boolean MineBlock(int difficulty) {        
        hackedBlock = null;
        hackedBlock = Block.generateBlock(blockChain.get(blockChain.size() - 1), difficulty, TXmempool, localSocketDirectory);
        if (Block.isBlockValid(hackedBlock, blockChain.get(blockChain.size() - 1))) {
            try {
                blockChain.add(hackedBlock);
                FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(hackedBlock), StandardCharsets.UTF_8,true);
                TXmempool.clear();
                return true;
            } catch (IOException ex) {
                LOGGER.error("Error while writing to dataFile.");
            }
        }
        return false;
    } 
    // </editor-fold>
}
