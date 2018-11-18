/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import Utils.StringUtil;
import blockchain.Block;
import blockchain.NodeWallet;
import blockchain.Transaction;
import blockchain.BlockchainDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.JOptionPane;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
public class StartRestServer
{
    private final String localHost = new StringUtil().getIP("enp0s3");
    private List<Block> blockChain = new ArrayList<>();
//    public BlockchainDB bc = new BlockchainDB();
    
    public void start() throws IllegalArgumentException, IOException
    {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        HttpServer server = HttpServerFactory.create("http://"+ localHost +":8080/api");
        server.start();
        
        GenesisBlockGenerator();
        
        System.out.println(new ObjectMapper().writeValueAsString(blockChain.get(0)));
        JOptionPane.showMessageDialog(null, "Press OK to shutdown server.");
        server.stop(0);
        
        
        
    }
    
    private void GenesisBlockGenerator () {      
        NodeWallet coinbase = new NodeWallet();
        NodeWallet myNode = new NodeWallet();
        Transaction genesisTransaction;
        genesisTransaction = new Transaction(coinbase.publicKey, myNode.publicKey, "genesis:data");
        genesisTransaction.timestamps = "2018-07-08 00:00:00";
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
        genesisTransaction.transactionId = "0";                          //manually set the transaction id        
        
        Block genesisBlock = new Block();
        genesisBlock.setPrevHash("0");
        genesisBlock.setIndex(0);
        genesisBlock.setTxAmount(1);
        genesisBlock.setDifficulty(3);
        genesisBlock.setTimestamp("2018-07-08 00:00:00"); // Project starting date
        genesisBlock.addTransaction(genesisTransaction);
        
        genesisBlock.setCreator(localHost);
        genesisBlock.setHash(genesisBlock.mineBlock());
        blockChain.add(genesisBlock);
    }

    @GET
    @Path("block/index/{index}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessage(@PathParam("index") int index) throws JsonProcessingException
    {
        System.out.println("\nReceived GET Request");
        System.out.println(blockChain.size());
        // Generate message
        Block sendBlock = blockChain.get(index);

        // Serialise Message
        ObjectMapper mapper = new ObjectMapper();
        String messageAsJSONstring = mapper.writeValueAsString(sendBlock);

        return messageAsJSONstring;
    }
}
