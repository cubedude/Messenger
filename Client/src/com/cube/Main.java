package com.cube;

import java.io.File;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;

import com.google.gson.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("=== Hello to Secure Chat v0.1 ===");
        System.out.println("=================================");
        String serverAddress = askUser("Please type server address.", false);
        String serverPort = askUser("Please type server port", false);
        System.out.println("=================================");
        String username = askUser("Please type desired username", false);
        System.out.println("=================================");

        Messenger mes = new Messenger(serverAddress, Integer.parseInt(serverPort), username);

    }

    public static String askUser(String text, boolean checkFile){
        String selected = "";
        String userChoice;

        Scanner userInput = new Scanner(System.in);

        while(selected.isEmpty()){
            System.out.println(text);

            userChoice = userInput.next();
            userChoice = userChoice.trim();
            if(!userChoice.isEmpty()){
                if(checkFile){
                    File f = new File(userChoice);
                    if(f.exists() && !f.isDirectory()) {
                        selected = userChoice;
                    }else{
                        System.out.println(".. "+userChoice+" is not valid...");
                    }
                }else{
                    selected = userChoice;
                }
            }
            else{
                System.out.println(".. "+userChoice+" is an invalid choice...");
            }
        }

        return selected;
    }
}
