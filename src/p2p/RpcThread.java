package p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Handling a single rpc connection
 * @author Mignet
 */
public class RpcThread extends Thread {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcThread.class);
	
    private Socket socket;
    public String res;
    public String req;

    /**
     * Default constructor
     * @param socket
     */
    public RpcThread(Socket socket){
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
                    out.println("#     5) filterblock <block fields> <value>- Filter blockchain based on block fields                          #");
                    out.println("#     6) filtertx <tx fields> <value>      - Filter transaction based on its fields                           #");
                    out.println("#     7) getaddr                           - Get my public key                                                #");
                    out.println("#     8) send <to address> <service:data>  - Send <data> in a transaction.                                    #");
                    out.println("#     9) mine <difficulty>                 - Mine <difficulty> with difficulty.                               #");
                    out.println("#     10)toggle-asm                        - Toggle autonomous selective mining mode.                         #");
                    out.println("#     11)toggle-am                         - Toggle autonomous mining mode.                                   #");
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
