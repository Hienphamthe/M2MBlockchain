/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package REST;

import java.util.Random;

public class Message
{
    private int id;
    private String timestamp;
    private String text;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }
    
    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
    
    public static Message generateExampleMessage()
    {
        Random random = new Random();

        Message message = new Message();
        message.setId(random.nextInt());
        message.setTimestamp(String.valueOf(random.nextInt()));
        message.setText("MessageText");

        return message;
    }

    @Override
    public String toString()
    {
        String outputMessage = String.format("Message: [id: %s, timestamp: %s, text: %s]", id, timestamp, text);
        return outputMessage;
    }
}

