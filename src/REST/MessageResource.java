/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import static blockchain.Main.backEnd;
import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import blockchain.Block;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("app")
public class MessageResource
{
    public final Gson gson = new GsonBuilder().create();
    public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    
    @GET
    @Path("block/index/{index}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessage(@PathParam("index") int index) throws JsonProcessingException
    {
        System.out.println("\nReceived GET Request");
        // Generate message
        Block sendBlock = backEnd.blockChain.get(index);

        // Serialise Message
        String messageAsJSONstring = gson.toJson(sendBlock);
        return messageAsJSONstring;
    }

    @POST
    @Path("peer/address")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String createMessage(String messageAsJSONstring) throws JsonParseException, JsonMappingException, IOException
    {
        System.out.println("\nReceived POST Request with string:\n" + messageAsJSONstring);
        String[] payload = messageAsJSONstring.split("@");
        backEnd.restClientNetwork.addPeer(payload[0], payload[1]);
        // Deserialise JSON message
//        ObjectMapper mapper = new ObjectMapper();
//        Message message = mapper.readValue(messageAsJSONstring, Message.class);
//        System.out.println("Creating Message Object...\n" + messageAsJSONstring);

        return "OK";
    }
    
//    @PUT
//    @Path("/{id}")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.TEXT_PLAIN)
//    public String updateMessage(@PathParam("id") String id, String messageAsJSONstring) throws JsonParseException, JsonMappingException, IOException
//    {
//        System.out.println("\nReceived PUT Request with JSON String:\n" + messageAsJSONstring + " and Parameter ID: " + id);
//
//        // Deserialise JSON message
//        ObjectMapper mapper = new ObjectMapper();
//        Message message = mapper.readValue(messageAsJSONstring, Message.class);
//        System.out.println("Updating Message Object with ID: " + id + "...\n" + message);
//
//        return "OK";
//    }
//
//
//    @DELETE
//    @Path("/{id}")
//    @Produces(MediaType.TEXT_PLAIN)
//    public String deleteMessage(@PathParam("id") int id)
//    {
//        System.out.println("\nReceived DELETE Request for Resource with ID: " + id);
//
//        System.out.println("Deleting Message with ID: " + id + "...");
//        return "OK";
//    }
}
