package blockchain;

import Utils.StringUtil;
import p2p.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

/**
 * Node controller (full blockchain, routing, wallet, miner/validator)
 * @author Student
 */
public class NodeMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeMain.class);
    
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
            }
            else {
                LOGGER.info("Input should be: frontend | backend");
            }
        }        
        LOGGER.info("End of main");
    }
}