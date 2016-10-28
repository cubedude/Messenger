package com.cube;

import java.io.File;
import java.util.Scanner;

public class Main {
    /*
        Main function that gets called out firstly
     */
    public static void main(String[] args) {
        //Define variables to use later
        boolean fastWay = false;
        String serverAddress;
        String serverPort;
        String username;

        //Welcome the user
        System.out.println("=================================");
        System.out.println("=== Hello to Secure Chat v0.1 ===");
        System.out.println("=================================");

        if(!fastWay){
            //Ask user data
            serverAddress = askUser("Please type server address: ", false);
            serverPort = askUser("Please type server port: ", false);
            System.out.println("=================================");
            username = askUser("Please type desired username: ", false);
            System.out.println("=================================");
        }
        else{
            //Quickdata for fast developing
            serverAddress = "localhost";
            serverPort = "500";
            username = "User";
        }

        //Start the messenger
        Messenger mes = new Messenger(serverAddress, Integer.parseInt(serverPort), username);

    }

    /*
       Function to ask user data.
       Option for it to be a file and then it checks if the file exists.
     */
    public static String askUser(String text, boolean checkFile){
        String selected = "";
        String userChoice;

        Scanner userInput = new Scanner(System.in);

        while(selected.isEmpty()){
            System.out.print(text);

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
