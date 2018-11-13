/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

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

import REST.Message;

@Path("message")
public class MessageResource
{

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessage() throws JsonProcessingException
    {
        System.out.println("\nReceived GET Request");
        // Generate message
        Message message = Message.generateExampleMessage();

        // Serialise Message
        ObjectMapper mapper = new ObjectMapper();
        String messageAsJSONstring = mapper.writeValueAsString(message);

        return messageAsJSONstring;
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String updateMessage(@PathParam("id") String id, String messageAsJSONstring) throws JsonParseException, JsonMappingException, IOException
    {
        System.out.println("\nReceived PUT Request with JSON String:\n" + messageAsJSONstring + " and Parameter ID: " + id);

        // Deserialise JSON message
        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(messageAsJSONstring, Message.class);
        System.out.println("Updating Message Object with ID: " + id + "...\n" + message);

        return "OK";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMessage(String messageAsJSONstring) throws JsonParseException, JsonMappingException, IOException
    {
        System.out.println("\nReceived POST Request with JSON String:\n" + messageAsJSONstring);

        // Deserialise JSON message
        ObjectMapper mapper = new ObjectMapper();
        Message message = mapper.readValue(messageAsJSONstring, Message.class);
        System.out.println("Creating Message Object...\n" + message);

        return "OK";
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String deleteMessage(@PathParam("id") int id)
    {
        System.out.println("\nReceived DELETE Request for Resource with ID: " + id);

        System.out.println("Deleting Message with ID: " + id + "...");
        return "OK";
    }
}
