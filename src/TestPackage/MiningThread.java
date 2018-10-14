/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TestPackage;

import blockchain.Block;
import blockchain.Transaction;
import java.util.List;

/**
 *
 * @author Mrhie
 */
public class MiningThread extends Object implements Runnable {
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
            System.out.println("interrupted in workMethod()");
        }
    }
    

    private void workMethod() throws InterruptedException {
        Thread.sleep(200);
        // blocks if suspended is true
        waitWhileSuspended();

        newBlock = Block.generateBlock(oldBlock, difficulty, transactions, creator);
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
        while (suspended) {
          Thread.sleep(200);
        }
    }
}
