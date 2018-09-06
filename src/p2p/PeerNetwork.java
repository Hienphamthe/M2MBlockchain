package p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The p2p network is responsible for handling connections and communications between peers. 
 * Processing each time from a separate thread
 * Each peerThreads contains 2 other threads: reader and writer
 * @author Mignet
 */
public class PeerNetwork extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerNetwork.class);
	
    private int listeningPort;
    private boolean runFlag = true;
    
    public List<PeerThread> peerThreads;
    public List<String> peersList;

    /**
     * Settings with port
     * @param port
     */
    public PeerNetwork(int port) {
    	this.listeningPort = port;
    	this.peerThreads = new ArrayList<>();
    	this.peersList = new ArrayList<>();
    }

    /** Open a listening socket
     * KEEP RUNNING TO RECEIVE CONNECTION
     * @param peerThread Open a new thread for each peer connection 
    */
    @Override
    public void run() {
        try {
            //PeerServerThread server = new PeerServerThread();
            //server.StartServer(listeningPort); 
            LOGGER.info("Node started at localport: "+listeningPort);
            ServerSocket listenSocket = new ServerSocket(listeningPort);            
            while (runFlag) 
            {
                Socket clientSocket = listenSocket.accept();
            	PeerThread peerThread = new PeerThread(clientSocket);                
                peerThread.start();   
                peerThreads.add(peerThread);
            }           
            //listenSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Establish connection to each peer on peerLists
     * @param host Peer to connect to
     * @param port Port on peer to connect to
    */    
    public void connect(String host, int port){
        Socket clientSocket;
    	try {
            clientSocket = new Socket(host, port);  
            
            String remoteHost = clientSocket.getInetAddress().getHostAddress();
            int remotePort = clientSocket.getPort();
            LOGGER.info("Connected to socket " + remoteHost + ":" + remotePort + " .");
            peersList.add(remoteHost + ":" + remotePort);
            

            PeerThread peerThread = new PeerThread(clientSocket);
            peerThread.start();
            peerThreads.add(peerThread);
        } catch (IOException e) {
            LOGGER.warn("socket " + host +":"+port+ " can't connect.");
        }
    }
        
    /**
     * Broadcast message to every peer on peerThreads list
     * @param data String to broadcast to peers
     */
    public void broadcast(String data) {
        for (PeerThread pt: peerThreads) {
            LOGGER.info("=> [p2p] BROADCAST: " + data);
            if( pt!=null){
            	pt.send(data);
            }
        }
    }
}