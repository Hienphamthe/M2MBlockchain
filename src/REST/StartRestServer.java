/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import javax.swing.JOptionPane;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;

public class StartRestServer
{
    public static void main(String[] args) throws IllegalArgumentException, IOException
    {
        HttpServer server = HttpServerFactory.create("http://localhost:8080/api");
        server.start();
        JOptionPane.showMessageDialog(null, "Press OK to shutdown server.");
        server.stop(0);
    }

}
