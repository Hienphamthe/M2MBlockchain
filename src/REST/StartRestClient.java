/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import blockchain.Block;

public class StartRestClient
{
    private String serverURL;

    public void start(String toIP) throws IOException
    {
        serverURL = "http://"+ toIP +":8080/api";
        
        doGetRequest();

//        doPostRequest();

//        doPutRequest();

//        doDeleteRequest();
    }




    private void doGetRequest() throws IOException
    {
        int index = 0;
        
        // Send GET request
        WebResource service = Client.create().resource(serverURL);
        String response = service.path("block/index/"+index).accept(MediaType.APPLICATION_JSON).get(String.class);

        System.out.println("Received JSON String:\n" + response);

        // Deserialise Message
        ObjectMapper mapper = new ObjectMapper();
        Block receivedBlock = mapper.readValue(response, Block.class);
        System.out.println("Creating Message Object...\n" + receivedBlock);
    }




    private void doPostRequest() throws JsonProcessingException
    {
        Message message = Message.generateExampleMessage();

        // Serialise Message Object
        ObjectMapper mapper = new ObjectMapper();

        String messageAsJSONstring = mapper.writeValueAsString(message);

        // Send POST request
        Client create = Client.create();
        WebResource service = create.resource(serverURL);
        String response = service.path("message").type(MediaType.APPLICATION_JSON).post(String.class, messageAsJSONstring);
        System.out.println(response);
    }




    private void doPutRequest() throws JsonProcessingException
    {
        Message message = Message.generateExampleMessage();

        // Serialise Message Object
        ObjectMapper mapper = new ObjectMapper();

        String messageAsJSONstring = mapper.writeValueAsString(message);

        // Send PUT request
        Client create = Client.create();
        WebResource service = create.resource(serverURL);
        String response = service.path("message").path(String.valueOf(message.getId())).type(MediaType.APPLICATION_JSON).put(String.class, messageAsJSONstring);
        System.out.println(response);
    }




    private void doDeleteRequest() throws JsonProcessingException
    {
        Message message = Message.generateExampleMessage();

        // Serialise Message Object
        ObjectMapper mapper = new ObjectMapper();

        String messageAsJSONstring = mapper.writeValueAsString(message);

        // Send DELETE request
        Client create = Client.create();
        WebResource service = create.resource(serverURL);
        String response = service.path("message").path(String.valueOf(message.getId())).type(MediaType.APPLICATION_JSON).delete(String.class, messageAsJSONstring);
        System.out.println(response);
    }
}

