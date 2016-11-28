package com.cube;

import java.io.File;
import java.util.Scanner;

public class Main {
    /*
        Main function that gets called out firstly
     */
    public static void main(String[] args) {
        //Define variables to use later, muutujad defineeritud, et hiljem kasutada
        boolean fastWay = false; //kui programm käivitatakse, küsib mis on ip port jne, aga arendades ei taha seda koguaeg kasutada
        // et ei peaks kogu aeg sisse logima, kui arendada, siis on true
        String serverAddress;
        String serverPort;
        String username;

        //Welcome the user
        System.out.println("=================================");
        System.out.println("=== Hello to Secure Chat v0.1 ===");
        System.out.println("=================================");

        //kui ei ole fastway, küsi kasutajalt andmeid, else kui on, aga siis ei küsi, aga siis on see kood
        if(!fastWay){
            //Ask user data
            serverAddress = askUser("Please type server address: ", false); // allpool tegelikult funktsioon, seda saab ka hiljem kasutada,
            // et failinimesid küsida, valideerimine on ka seal olemas, teeb nii, et ei saa valet asja panna
            serverPort = askUser("Please type server port: ", false); // see on sama funktsioon, mis eelpool
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

        //Start the messenger, messenger klass on new messenger ja sisestad andmed
        Messenger mes = new Messenger(serverAddress, Integer.parseInt(serverPort), username);

    }

    /*
       Function to ask user data.
       Option for it to be a file and then it checks if the file exists.
     */
    public static String askUser(String text, boolean checkFile){ //küsib failinime ja küsib kas reaalselt olemas ka
        String selected = ""; // muutujad, mida kasutatakse hiljem, see on see, mis läheb lõpus tagastamisele
        String userChoice; // placeholder muutuja, mida kasutatakse while loopi sees

        Scanner userInput = new Scanner(System.in); // võimaldab küsida user inputi

        //while loopis kuni selected on tühi, nii kaua küsi kasutaja käes uuesti andmeid
        while(selected.isEmpty()){
            System.out.print(text); //iga kord uut küsimust esitada samafunktsiooni abil

            userChoice = userInput.next();
            userChoice = userChoice.trim(); // võtab tühikud algusest ja lõpust ära
            if(!userChoice.isEmpty()){ // kui ei ole tühi
                if(checkFile){ // kui see on fail, siis vaadatakse kas eksisteerib ja see ei ole kaust
                    File f = new File(userChoice);
                    if(f.exists() && !f.isDirectory()) { // exists ja Directory on java sisseehitatud funktsioonid
                        selected = userChoice;              // kui on  läbinud kõik kontrollid, siis selected muutujale omistatakse väärtus
                    }else{                  // kuna omistati väärtus, siis while loop lõpeb, sest selected ei ole enam tühi
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
