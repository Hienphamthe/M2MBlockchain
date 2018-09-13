package p2p;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OutputThread is responsible for writing data to the peer
 * @author Mignet
 */
public class PeerWriter extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PeerWriter.class);

	private final Socket socket;
	private ArrayList<String> outputBuffer;
	private boolean runFlag = true;

	public PeerWriter(Socket socket) {
            this.socket = socket;
	}

	@Override
	public void run() {            
            try {
                LOGGER.info("Start writer thread!");
                outputBuffer = new ArrayList<>();
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                while (runFlag) {
                    if (!outputBuffer.isEmpty() && outputBuffer.get(0) != null) {
                        outputBuffer.forEach(out::println);
                        outputBuffer = new ArrayList<>();
                        outputBuffer.add(null);
                    }
                    outputBuffer = new ArrayList<>();
                    outputBuffer.add(null);
                    TimeUnit.MILLISECONDS.sleep(200);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.info("Peer " + socket.getInetAddress().getHostAddress()+":"+socket.getPort() + " disconnected."+e.getMessage()); 
            }
	}

	/**
	 * Write buffer
	 *
	 * @param data Data to write
	 */
	public void write(String data) {
            if (!outputBuffer.isEmpty()) {
                if (outputBuffer.get(0) == null) {
                    outputBuffer.remove(0);
                }
            }
            outputBuffer.add(data);
	}

	/**
	 * 
	 */
	public void shutdown() {
            runFlag = false;
	}
}