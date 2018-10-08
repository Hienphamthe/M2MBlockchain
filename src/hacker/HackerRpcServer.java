/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hacker;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p2p.RpcThread;

/**
 *
 * @author Mrhie
 */
public class HackerRpcServer extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HackerRpcServer.class);
    private int port;
    private boolean runFlag = true;

    public List<HackerRpcThread> hackerRpcThreads;

    /**
     * Designated port
     * @param port Port to listen on
     */
    public HackerRpcServer(int port)
    {
        this.port = port;
        this.hackerRpcThreads = new ArrayList<>();
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
            	HackerRpcThread thread = new HackerRpcThread(socket.accept());
                hackerRpcThreads.add(thread);
                thread.start();
            }
            socket.close();
        } catch (Exception e){
        	LOGGER.error("RPC error in port:" + port,e);
        }
    }
}