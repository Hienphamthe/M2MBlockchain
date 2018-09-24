package p2p;

import blockchain.BackEnd;
import java.io.*;
import java.net.*;
/**
 *
 * @author Student
 */
public class FrontEnd {
    public static int port = BackEnd.localPort+1;
    public static BufferedReader con_br = new BufferedReader(new InputStreamReader(System.in));

    public void startFrontend() throws IOException {
        String address = BackEnd.localHost;
        Socket sock = new Socket(address, port);
        BufferedReader sock_br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        PrintWriter sock_pw = new PrintWriter(sock.getOutputStream(), true);
        System.out.println("Connection established");

        Thread chat_client_writer = new ChatWriter(sock_pw, con_br);
        chat_client_writer.start();

        String s;
        while((s = sock_br.readLine()) != null)
        {
            System.out.println("\rserver: " + s);
            System.out.print("> ");
        }
        sock.close();
    }
}

class ChatWriter extends Thread
{
    BufferedReader con_br;
    PrintWriter sock_pw;

    public ChatWriter(PrintWriter sock_pw, BufferedReader con_br)
    {
        this.sock_pw = sock_pw;
        this.con_br = con_br;
    }

    public void run()
    {
        String s;
        try
        {
            while(true)
            {
                System.out.print("> ");
                s = con_br.readLine();
                if(s != null)
                    sock_pw.println(s);
                else
                    break;
            }
        }
        catch(Exception e)
        {System.err.println("chat_server_writer: Exception occured:\n" + e);}
    }
}