package p2p;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC service
 *
 * Note: Do not open this port to the external network
 * @author Mignet
 */
public class RpcServer extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);
    private int port;
    private boolean runFlag = true;

    public List<RpcThread> rpcThreads;

    /**
     * Designated port
     * @param port Port to listen on
     */
    public RpcServer(int port)
    {
        this.port = port;
        this.rpcThreads = new ArrayList<>();
    }

    @Override
    public void run()
    {
        try
        {
            ServerSocket socket = new ServerSocket(port);
            LOGGER.info("RPC agent is Started in port:"+port);
            while (runFlag)
            {
            	RpcThread thread = new RpcThread(socket.accept());
                rpcThreads.add(thread);
                thread.start();
            }
            socket.close();
        } catch (Exception e){
        	LOGGER.error("RPC error in port:" + port,e);
        }
    }
}