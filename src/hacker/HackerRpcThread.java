/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hacker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mrhie
 */
public class HackerRpcThread extends Thread {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HackerRpcThread.class);
	
    private Socket socket;
    public String res;
    public String req;

    /**
     * Default constructor
     * @param socket
     */
    public HackerRpcThread(Socket socket){
        this.socket = socket;
        LOGGER.info("An RPC client has connected.");
    }

    @Override
    public void run(){
        try{
            req = null;
            res = null;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            out.println("[   Welcome RPC Daemon    ]");
            while((input = in.readLine()) != null){
                if ("HELP".equalsIgnoreCase(input)){
                    out.println("################################################# COMMANDS ####################################################");
                    out.println("#     1) getinfo                           - Get Blockchain infomation.                                       #");
                    out.println("#     2) createwallet                      - Create a new wallet address.                                     #");
                    out.println("#     3) unlock <publickey> <privatekey>   - Unlock existing wallet with private and public key.              #");
                    out.println("#     4) getpendingtx                      - Gets all pending transactieons.                                  #");
                    out.println("#     5) getaddr                           - Get my public key                                                #");
                    out.println("#     6) send <to address> <service:data>  - Create a legit transaction.                                      #");
                    out.println("#     7) mine <difficulty>                 - Create a legit block.                                            #");
                    out.println("#     8) maltx_key                         - Send manipulated tx with new recipient                           #");
                    out.println("#     9) maltx_message                     - Send manipulated tx with new message                             #");
                    out.println("#     10) malblock_nonce                   - Send manipulated block with new nonce                            #");
                    out.println("#     11) malblock_txmessage               - Send manipulated block with new tx message                       #");
                    out.println("###############################################################################################################");
                } else {
                    req = input;
                    while (res == null){
                        TimeUnit.MILLISECONDS.sleep(25);
                    }
                    out.println(res);
                    req = null;
                    res = null;
                }
            }
        } catch (Exception e){
            LOGGER.info("An RPC client has disconnected.");
        }
    }
}

