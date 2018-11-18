/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blockchain;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mrhie
 */
public class BlockchainDB {
    private List<Block> blockChain = new ArrayList<>();
    
    public void writeBC (Block blockIn) {
        blockChain.add(blockIn);
    }
    
    public Block readBC (int index) {
        return blockChain.get(index);
    }
}
