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
                    out.println("################################################# COMMANDS ##################################################");
                    out.println("#     1) getinfo                           - Gets block chain infomations.                                  #");
                    out.println("#     1) createwallet                      - Create a new wallet address.                                   #");
                    out.println("#     1) unlock <publickey> <privatekey>   - Create a new wallet address.                                   #");
                    out.println("#     2) getpendingtx                      - Gets all pending transactions.                                 #");
                    out.println("#     2) filterblock <block fields> <value>- Filter blockchain based on block fields                        #");
                    out.println("#     2) filtertx <tx fields> <value>      - Filter transaction based on its fields                         #");                    
                    out.println("#     2) getaddr                           - Gets my public key                                             #");
                    out.println("#     3) send <to address> <data/report>   - Send <data/report> in atransaction.                            #");
                    out.println("#     3) buildreport                       - Build a report.                                                #");
                    out.println("#                                            Type \"done\" when finish report                                 #");
                    out.println("#                                            Report syntax <serviceName>:<comment>                          #");
                    out.println("#     4) mine <difficulty>                 - Mine <difficulty> with Block.                                  #");
                    out.println("#############################################################################################################");
                } else {
                    req = input;
//                    if (input.equals("buildreport")){
//                        out.println("Start building report.");
//                        req = null;
//                        res = null;
//                    } else {
                        while (res == null){
                            TimeUnit.MILLISECONDS.sleep(25);
                        }
                        out.println(res);
                        req = null;
                        res = null;
//                    }                    
                }
            }
        } catch (Exception e){
            LOGGER.info("An RPC client has disconnected.");
        }
    }
}
