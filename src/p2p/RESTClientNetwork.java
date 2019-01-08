package p2p;

import REST.StartRestClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTClientNetwork extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RESTClientNetwork.class);
    private StartRestClient RESTclient = new StartRestClient();
    private List<String> peerList = new ArrayList<>();
    //Buffer
    private ArrayList<String> receivedData = new ArrayList<>();
    
    public void addPeer(String peerHost, String peerPort) {
        peerList.add("http://"+ peerHost +":"+ peerPort +"/app/");
    }
    
    public void intro (String address) {
        if (!peerList.isEmpty()) {
            for (String pt: peerList) {
                if(pt!=null) RESTclient.doPostRequest(pt, "peer/address", address);
            }
        }
    }
    
    public void broadcastGET (String path) {
        if (!peerList.isEmpty()) {
            for (String pt: peerList) {
                if(pt!=null) try {
                    receivedData.add(RESTclient.doGetRequest(pt, path));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public void broadcastPOSTBlockHeight(int blockheight){
        if (!peerList.isEmpty()) {
            for (String pt: peerList) {
                if(pt!=null) RESTclient.doPostRequest(pt, "", String.valueOf(blockheight));
            }
        }
    }
    
    public List<String> readGETData() {
        ArrayList<String> inputBuffer = new ArrayList<>(receivedData);
        receivedData.clear(); //clear 'buffer'
        return inputBuffer;
    }
//    private boolean runFlag = true;
//    
//    public List<String> peersList;
//
//    @Override
//    public void run() {
//        try {
//            //PeerServerThread server = new PeerServerThread();
//            //server.StartServer(listeningPort); 
//            listenSocket = new ServerSocket(peerList);  
//            while (runFlag) 
//            {
//                Socket clientSocket = listenSocket.accept();
//            	PeerThread peerThread = new PeerThread(clientSocket);                
//                peerThread.start();   
//                peerThreads.add(peerThread);                
//            }           
//            //listenSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//        
//    /**
//     * Broadcast message to every peer on peerThreads list
//     * @param data String to broadcast to peers
//     */
//    public void broadcast(String data) {
//        if (!peerThreads.isEmpty()) {
//            LOGGER.info("=> [p2p] BROADCAST: " + data);
//            for (PeerThread pt: peerThreads) {
//                if(pt!=null) pt.send(data);
//            }
//        }
//    }
}