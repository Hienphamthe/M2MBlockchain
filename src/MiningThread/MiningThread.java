/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MiningThread;

import blockchain.Block;
import blockchain.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mrhie
 */
public class MiningThread extends Object implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningThread.class);
    private final AtomicBoolean running = new AtomicBoolean(false);
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();     
    
    private volatile Block oldBlock;
    private volatile int difficulty;
    private volatile List<Transaction> transactions;
    private volatile String creator;
    
    public volatile Block newBlock;
    
    private volatile boolean suspended;
    @Override
    public void run() {
        try {
            suspended = false;
            workMethod();
        } catch (InterruptedException x) {
            System.out.println("Interrupted in workMethod()");
        }
    }
    
    
    

    private void workMethod() throws InterruptedException {
        Thread.sleep(500);
        waitWhileSuspended();

        newBlock = Block.generateBlock(oldBlock, difficulty, transactions, creator);
        
        Thread.sleep(500);
    }

    public void suspendRequest() {
        suspended = true;
    }
    public void resumeRequest() {
        suspended = false;
    }
    public void setVar(Block a, int b, List<Transaction> c, String d) {
        oldBlock = a;
        difficulty = b;
        transactions = c;
        creator = d;
    }
    private void waitWhileSuspended() throws InterruptedException {
        LOGGER.info("Thread is suspended.");
        while (suspended) {
          Thread.sleep(500);
        }
    }
}
