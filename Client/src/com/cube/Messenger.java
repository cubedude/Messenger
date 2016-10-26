package com.cube;

import com.google.gson.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class Messenger {
    private String _username;

    Messenger (String serverAddress, int serverPort, String username){
        Socket s;
        OutputStream out;
        InputStream in;
        byte[] buffer = new byte[1024];

        _username = username;

        try {
            //Make a connection
            s = new Socket( serverAddress, serverPort );
            out = s.getOutputStream();
            in = s.getInputStream();
            String input;
            System.out.println(".. connected to the server.");
            System.out.println(".. joined to public chat. Use /help to see commands.");

            //Register to the server
            String registers = processCommand("/register "+username);
            out.write(registers.getBytes());

            while (true) {
                int l;
                if (System.in.available() != 0)
                {
                    l = System.in.read( buffer );
                    if (l == -1) break;

                    input = processCommand(new String(buffer, "UTF-8"));

                    if(input != null){
                        out.write(input.getBytes());
                        input = null;
                    }
                }

                if (in.available() != 0)
                {
                    l = in.read( buffer, 0, buffer.length );
                    processResponse(new String(buffer, "UTF-8"));
                }

                Thread.currentThread().sleep( 200 ); // 100 milis
            }
        } catch (Exception e) {
            System.err.println( "Exception: " + e );
        }
    }

    String processCommand(String input){
        String output = null;
        input = input.replace("\n", "");
        String[] splited = input.split(" ");

        String c = String.valueOf(splited[0].charAt(0));
        if(c.equals("/")){
            if(splited[0].equals("/?") || splited[0].equals("/help"))
            {
                System.out.println("=================================");
                System.out.println("Available commands:");
                System.out.println("/help - display available commands");
                System.out.println("/register \"username\" - Changes your name on server");
                System.out.println("/list - see the list of users on the server");
                System.out.println("\"message\" - send message to everyone on the server");
                System.out.println("@username \"message\" send message to specific username");
                System.out.println("=================================");
            }
            else if(splited[0].equals("/set"))
            {
                //Set security level for chat
                System.out.println(".. set command not implemented!");
            }
            else if(splited[0].equals("/reg") || splited[0].equals("/register"))
            {
                //Register username
                if(splited[1].isEmpty()){
                    System.out.println(".. username can't be empty!");
                }else{
                    _username = splited[1];
                    JsonObject register = new JsonObject();
                    register.addProperty("command","register");
                    register.addProperty("src",_username);

                    output = register.toString();
                }
            }
            else if(splited[0].equals("/list")) {
                //Check who is online
                System.out.println(".. list not implemented!");
            }
            else{
                System.out.println(".. \""+splited[0]+"\" is not a valid command!");
            }
        }
        else if(c.equals("@")) {
            // Send message to specific person
            System.out.println(".. send message to \""+splited[0].substring(1)+"\" not implemented!");
        }else{
            // Send message to ALL
            System.out.println(".. send message to all not implemented!");
        }

        return output;
    }

    void processResponse(String input){
        input = input.replace("\n", "");
        try{
            JsonObject resp = new JsonParser().parse(input).getAsJsonObject();
            if (resp.isJsonObject()) {
                JsonElement error = resp.get("error");
                JsonElement result = resp.get("result");

                if (error != null) {
                    System.out.println(error.toString());
                }
                if (result != null){
                    System.out.println(result.toString());
                }
            }
            else{
                System.err.println( "Error while reading command from server: "+input );
            }
        } catch (Exception e) {
            System.err.println( "Error while reading JSON command from server: "+input );
            System.err.println(e);
        }
    }

}
