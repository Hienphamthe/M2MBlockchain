package p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InputThread only reads data from the peer node.
 * All data read is stored in an ArrayList, and each row is stored separately.
 * Access data through a channel through PeerNetwork.
 * @author Mignet
 */
public class PeerReader extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerReader.class);	
    private Socket clientSocket;

    //Buffer
    private ArrayList<String> receivedData = new ArrayList<>();

    /**
     * Incoming socket
     * @param socket
     */
    public PeerReader(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {        
        try {
            LOGGER.info("Start reader thread!");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input;
            while ((input = in.readLine()) != null) {
                receivedData.add(input);
            }
        } catch (Exception e) {
        	LOGGER.error("Peer " + clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort() + " disconnected." +e.getMessage());
        }
    }

    /**
     * Take out buffered data
     * @return List<String> Data pulled from receivedData
     */
    public List<String> readData() {
        ArrayList<String> inputBuffer = new ArrayList<>(receivedData);
        receivedData.clear(); //clear 'buffer'
        return inputBuffer;
    }
}