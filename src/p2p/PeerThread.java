package p2p;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P2p communication thread
 * After accepting the socket, it is divided into two separate threads, one for input data and one for output data, so one-way data will not block
 * @author Mignet
 */
public class PeerThread extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerThread.class);
    private final Socket clientSocket;
    public PeerReader peerReader;
    public PeerWriter peerWriter;
    
    /**
     * Constructor
     * @param clientSocket Socket of requesting peer
     */
    public PeerThread(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run()
    {
    	LOGGER.info("Got connection from " + clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort()+" .");
        peerReader = new PeerReader(clientSocket);
        peerReader.start();
        peerWriter = new PeerWriter(clientSocket);
        peerWriter.start();
    }
    /**
     * Send data
     * @param data String of data to send
     */
    public void send(String data)
    {
        if (peerWriter == null)
        {
            LOGGER.error("Couldn't send " + data + " when outputThread is null");
        }
        else
        {
            peerWriter.write(data);
        }
    }
    
    public Socket getClientSocket(){
        return clientSocket;
    }
}