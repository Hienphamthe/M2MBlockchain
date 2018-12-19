/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import blockchain.Block;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StartRestClient {
//    private String serverURL;
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    public void start(String toIP) throws IOException {
//        serverURL = "http://"+ toIP +":8080/api";
        
//        doGetRequest();

//        doPostRequest();

//        doPutRequest();

//        doDeleteRequest();
    }




    public String doGetRequest(String target, String path) throws IOException {
        // Send GET request
        WebResource service = Client.create().resource(target);
        String response = service.path(path).accept(MediaType.APPLICATION_JSON).get(String.class);
        
        return response;
//        System.out.println("Received JSON String:\n" + response);
//
//        // Deserialise Message
//        Block receivedBlock = gson.fromJson(response, Block.class);
//        System.out.println("Creating Message Object...\n" + receivedBlock);
    }


    public void doPostRequest(String target, String path, String message) {
        Client create = Client.create();
        WebResource service = create.resource(target);
        String response = service.path(path).type(MediaType.TEXT_PLAIN).post(String.class, message);
        System.out.println(response);
    }




//    private void doPutRequest(String target, String path) {
//        Message message = Message.generateExampleMessage();
//
//         Serialise Message Object
//        String messageAsJSONstring = gson.toJson(message);
//
//        // Send PUT request
//        Client create = Client.create();
//        WebResource service = create.resource(serverURL);
//        String response = service.path("message").path(String.valueOf(message.getId())).type(MediaType.APPLICATION_JSON).put(String.class, messageAsJSONstring);
//        System.out.println(response);
//    }




//    private void doDeleteRequest(){
////        Message message = Message.generateExampleMessage();
//
//        // Serialise Message Object
//        String messageAsJSONstring = "";
//
//        // Send DELETE request
//        Client create = Client.create();
//        WebResource service = create.resource(serverURL);
//        String response = service.path("message").path(String.valueOf(message.getId())).type(MediaType.APPLICATION_JSON).delete(String.class, messageAsJSONstring);
//        System.out.println(response);
//    }
}

