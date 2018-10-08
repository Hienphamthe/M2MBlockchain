package blockchain;

import hacker.*;
import p2p.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node controller (full blockchain, routing, wallet, miner/validator)
 * @author Student
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.print("Start as: ");
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String s = userInput.readLine();
        if (s!=null) {
            if(s.equalsIgnoreCase("frontend")){
                // backend must finish initializing before staring frontend
                new FrontEnd().startFrontend();
            } else if (s.equalsIgnoreCase("backend")) {
                new BackEnd().startBackend();
            } else if (s.equalsIgnoreCase("testerB")) {
                new HackerBackEnd().startBackend();
            } else if (s.equalsIgnoreCase("testerF")) {
                new FrontEnd().startFrontend();
            }
            else {
                LOGGER.info("Input should be: frontend | backend | testerB | testerF");
            }
        }        
        LOGGER.info("End of main");
    }
}