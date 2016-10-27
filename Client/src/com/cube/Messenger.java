package com.cube;

import com.google.gson.*;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;


public class Messenger {
    private String _username = null;
    private String partner = null;
    private Queue commands = new LinkedList();
    private boolean verbose;

    Messenger (String serverAddress, int serverPort, String username){
        Socket s;
        OutputStream out;
        InputStream in;
        byte[] buffer = new byte[1024];

        verbose = false;

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

                    input = null;
                    input = processCommand(new String(buffer, "UTF-8"));
                    buffer = new byte[1024];

                    if(input != null){
                        out.write(input.getBytes());
                    }
                }

                if (in.available() != 0)
                {
                    l = in.read( buffer, 0, buffer.length );
                    processResponse(new String(buffer, "UTF-8"));
                    buffer = new byte[1024];
                }

                Thread.currentThread().sleep( 200 ); // 100 milis
            }
        } catch (Exception e) {
            System.err.println( "Exception: " + e );
        }
    }

    String processCommand(String input){
        String output = null;
        input = input.replace("\n", "").trim();
        if(input.isEmpty()) return output;

        String[] splited = input.split(" ");

        String c = String.valueOf(splited[0].charAt(0));
        if(c.equals("/")){
            if(splited[0].equals("/?") || splited[0].equals("/help"))
            {
                System.out.println("=================================");
                System.out.println("Available commands:");
                System.out.println("/help - display available commands");
                System.out.println("/verbose - toggle verbose mode on and off");
                System.out.println("/register \"username\" - Changes your name on server");
                System.out.println("/list - see the list of users on the server");
                System.out.println("\"message\" - send message to everyone on the server");
                System.out.println("@username \"message\" send message to specific username");
                System.out.println("=================================");
            }
            else if(splited[0].equals("/v") || splited[0].equals("/verbose"))
            {
                if(verbose){
                    verbose = false;
                    System.out.println("System: Verbose mode turned OFF");
                }else{
                    verbose = true;
                    System.out.println("System: Verbose mode turned ON");
                }
            }
            else if(splited[0].equals("/reg") || splited[0].equals("/register"))
            {
                //Register username
                if(splited.length <= 1 || splited[1].trim().isEmpty()){
                    System.out.println("System: username can't be empty!");
                }
                else if(splited[1].trim().toLowerCase().equals("all")){
                    System.out.println("System: username can't be \"All\"!");
                }else{
                    _username = splited[1];
                    JsonObject register = new JsonObject();
                    register.addProperty("command","register");
                    register.addProperty("src",_username);

                    output = register.toString();

                    if(verbose) System.out.println("System: Add \"register\" to commands");
                    commands.add("register");
                }
            }
            else if(_username.equals(null)) {
                System.out.println("System: set username with /register command!");
            }
            else if(splited[0].equals("/list")) {
                //Check who is online
                JsonObject listing = new JsonObject();
                listing.addProperty("command","list");
                listing.addProperty("src",_username);
                output = listing.toString();

                if(verbose) System.out.println("System: Add \"list\" to commands");
                commands.add("list");
            }
            else if(splited[0].equals("/all")) {
                // Send message to All
                String messages = String.join(" ", Arrays.copyOfRange(splited, 1, splited.length));

                partner = "";
                if(!messages.isEmpty()) {
                    JsonObject message = new JsonObject();
                    message.addProperty("command", "send");
                    message.addProperty("src",_username);
                    message.addProperty("message", messages);
                    output = message.toString();

                    if(verbose) System.out.println("System: Add \"send\" to commands");
                    commands.add("send");
                }
            }
            else{
                System.out.println("System: \""+splited[0]+"\" is not a valid command!");
            }
        }
        else if(_username.equals(null)) {
            System.out.println("System: set username with /register command!");
        }
        else if(c.equals("@")) {
            // Send message to specific person
            String messages = String.join(" ", Arrays.copyOfRange(splited, 1, splited.length));

            partner = splited[0].substring(1).trim();

            if(!messages.isEmpty()) {
                JsonObject message = new JsonObject();
                message.addProperty("command", "send");
                message.addProperty("src", _username);
                message.addProperty("message", messages);
                message.addProperty("dst", partner);
                output = message.toString();

                if(verbose) System.out.println("System: Add \"send\" to commands");
                commands.add("send");
            }
        }else{
            // Send message to last person
            String messages = String.join(" ", splited);
            if(!messages.isEmpty()){
                JsonObject message = new JsonObject();
                message.addProperty("command","send");
                message.addProperty("src",_username);
                message.addProperty("message",messages);
                if(!partner.isEmpty()) message.addProperty("dst",partner);
                output = message.toString();

                if(verbose) System.out.println("System: Add \"send\" to commands");
                commands.add("send");
            }
        }

        return output;
    }

    void processResponse(String input){
        input = input.replace("\n", "").trim();
        try{
            JsonObject resp = new JsonParser().parse(input).getAsJsonObject();
            if (resp.isJsonObject()) {
                JsonElement errors = resp.get("error");
                JsonElement result = resp.get("result");
                JsonElement message = resp.get("message");
                JsonElement dst = resp.get("dst");
                JsonElement src = resp.get("src");

                if (src != null) {
                    //Message from someone
                    System.out.print("@"+src.getAsString()+": ");

                    if (message != null) {
                        System.out.println(message.getAsString());
                    }
                }
                if (errors != null) {
                    //Message from server
                    String error = errors.getAsString().trim();
                    String latest = commands.remove().toString();

                    if(verbose) System.out.println("System: last command was: "+latest);

                    if(latest.equals("send")){
                        if(error.equals("ok")){
                            if(verbose) System.out.println("System: message deliverd");
                        }
                        else if(error.equals("unknown")){
                            System.out.println("Server: Wrong username.");
                        }
                        else{
                            System.out.println("Server: Unexpected response for messageing: "+error);
                        }
                    }
                    else if(latest.equals("register")){
                        if(error.equals("ok") || error.equals("registered")){
                            if(verbose) System.out.println("Server: registerd!");
                        }
                        else if(error.equals("re-registered")){
                            System.out.println("Server: Name changed!");
                        }
                        else{
                            System.out.println("Server: Unexpected response for /register: "+error);
                        }
                    }
                    else if(latest.equals("list")){
                        if(error.equals("ok")){
                            JsonArray users = result.getAsJsonArray();
                            List userList = new ArrayList();

                            for(JsonElement user : users){
                                JsonObject usr = user.getAsJsonObject();
                                if(usr != null){
                                    userList.add(usr.get("src").getAsString());
                                }
                            }

                            if(userList.isEmpty()){
                                System.out.println("Server: No users are online");
                            }
                            else{
                                System.out.println("Server: Users online: "+String.join(", ",userList));
                            }
                        }
                        else{
                            System.out.println("Server: Unknown response for list: "+error);
                            if (result != null){
                                System.out.println("Result: "+result.toString());
                            }
                        }
                    }
                    else{
                        System.out.println("Server: Unknown response: "+error);
                        if (result != null){
                            System.out.println("Result: "+result.toString());
                        }
                    }
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