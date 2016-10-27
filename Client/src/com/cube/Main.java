package com.cube;

import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        boolean fastWay = false;
        String serverAddress;
        String serverPort;
        String username;

        System.out.println("=================================");
        System.out.println("=== Hello to Secure Chat v0.1 ===");
        System.out.println("=================================");

        if(!fastWay){
            serverAddress = askUser("Please type server address.", false);
            serverPort = askUser("Please type server port", false);
            System.out.println("=================================");
            username = askUser("Please type desired username", false);
            System.out.println("=================================");
        }
        else{
            serverAddress = "localhost";
            serverPort = "500";
            username = "User";
        }

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
