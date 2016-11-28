package com.cube;

//Imports for detecting inputs
import java.io.InputStream;
import java.io.OutputStream;

//Imports for connecting to server
import java.net.Socket;

//Imports for arrays and lists used
import java.util.*;

//Import for json
import com.google.gson.*;

//Imports for encryption and decryption
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

//Imports for helping functions
import org.apache.commons.codec.binary.Base64;

public class Messenger {
    //Define variables
    private String _username = "";
    private String partner = "";
    private Queue commands = new LinkedList(); // FIFO, LILO
    private boolean verbose = false;

    private Map<String, HashMap> modeList = new HashMap<String, HashMap>(); // Setted modes
    private Map<String, HashMap> tempModeSrc = new HashMap<String, HashMap>(); // temporary modes for user
    private Map<String, HashMap> tempModeDest = new HashMap<String, HashMap>(); // temporary modes from other

    // Settings for encryptions
    private String passwordCypherScheme = "AES";
    private String passwordCypherSchemeLong = "AES/CBC/PKCS5Padding";

    /*
        Messenger construction to set up the connection to server
    */
    Messenger (String serverAddress, int serverPort, String username){
        //vanad variables
        Socket s;       // Pesa sisseehitatud klass
        OutputStream out; // sisseehitatud klass
        InputStream in; //sama
        byte[] buffer = new byte[1024];

        try {
            // Make a connection, proovi, kas ühendust saab teha
            s = new Socket( serverAddress, serverPort );
            out = s.getOutputStream(); // socketi funktsioon, hakkab lugema serveri poolt tulevat infot
            in = s.getInputStream(); // loeb kasutajapoolset infot
            String input;
            System.out.println(".. connected to the server.");
            System.out.println(".. joined to public chat. Use /help to see commands.");

            // Register to the server
            String registers = processCommand("/register "+username); // funktsioon, mis saadab serverisse funtksiooni / register kasutajanimi
            out.write(registers.getBytes()); // kui proces command on teinud selle rea serverile mõistetavaks, siis ta saadab selle
            //baitide formaadis serverise

            // Start monitoring input from the user and the server - infinite loop, mis ei lõpe kunagi ära, ei saa kunagi olla false
            // küsib kas õige on õige

            //kuidas käitub kui tahab ühendust saavutada
            while (true) {
                int l;
                if (System.in.available() != 0) // kontrollib kas kasutaja on sisestanud mingit infot
                {
                    // Check if user send a command
                    l = System.in.read( buffer ); // loeb commandi in-st
                    if (l == -1) break; // see on vale, katkestab, alustab alguses

                    // Parse the command
                    input = null;           // igaks juhuks nullib inputi ära
                    input = processCommand(new String(buffer, "UTF-8")); // sama mis registerCommand, mida saab korduvalt kasutada
                    buffer = new byte[1024]; //nullib bufferi, seda kasutati enne kasutajaandmete saamiseks, on vaja tühja

                    // If command has something to send to the server
                    if(input != null){  // kui input ei ole 0, kui process command tagastas midagi, siis saada serverisse
                        out.write(input.getBytes()); // kui ei tagastanud, sii spole midgai saata
                    }
                }

                if (in.available() != 0) // kui serverist tuleb mingit infot, siis loe bufferisse see, mida server saatis
                {
                    // If theres something from the server
                    l = in.read( buffer, 0, buffer.length );
                    // Process the message
                    input = null;
                    input = processResponse(new String(buffer, "UTF-8")); // loeb serverist saanud informatsiooni
                    buffer = new byte[1024]; // uuesti nullib bufferi ära, ei ole vaja segada info saamist

                    // If theres an automatic response needed to be sent to the server
                    if(input != null){
                        out.write(input.getBytes());
                    }
                }

                // Wait for a bit to check again pm on see sleep
                Thread.currentThread().sleep( 200 ); // 100 milis , sisseehitatud funktsioon, saab ka multithreadida
            }
        } catch (Exception e) {             // kui ei saanud serverisse connectionit teha, siis tagastab errorit
            System.err.println( "Exception: " + e );
        }
    }

    /*
        Process command from the user and convert them to server json if required
        ProcessCommand töötleb infot, mis on saadud userilt ja convert selle Json-iks kui vaja
    */
    String processCommand(String input){        // kui kasutajalt tuleb sisendit, siis see tuleb töödelda
        String[] splited;                       // defineeritakse muutujad mida läheb vaja
        String output = null;                   // väljund kui serverile tagastatakse, siis see on placeholder
        input = input.replace("\n", "").trim(); // replace võtab enterid ära ja tühikud
        if(input.isEmpty()) return output;      // kui sisend on tühi, siis tagastab 0, kuna pole midagi töödelda

        // try to split the message to seperate command, tehtud selleks et ei oleks erroreid, võibolla pole seda vaja
        try {
            splited = input.split(" ");         // siin splitib tühiku järgi, proovib seda teha
        }
        catch (Exception e) {
            splited = new String[1];
            splited[0] = input;
        }

        String c = String.valueOf(splited[0].charAt(0)); // on kõige esimene karakter, mida kasutaja sisestas

        // if user input seems to have inserted commmand
        if(c.equals("/")){                  // kas on võrdne /, siis kasuataja püüdis sisestada mingit commandi
            if(splited[0].equals("/?") || splited[0].equals("/help")) // kui kontrollib neid asju,
            {
                // Display help for the user, kontrollime, mis commandi kasutaja sisestas
                System.out.println("=================================");
                System.out.println("Available commands:");
                System.out.println("/help - display available commands");
                System.out.println("/verbose - toggle verbose mode on and off");
                System.out.println("/register \"username\" - Changes your name on server");
                System.out.println("/list - see the list of users on the server");
                System.out.println("/setmode \"username\" \"mode\" - set securiy mode for username");
                System.out.println("/modelist - get available securiy modes");
                System.out.println("/debug - debug, show some random data");
                System.out.println("\"message\" - send message to everyone on the server");
                System.out.println("@username \"message\" send message to specific username");
                System.out.println("=================================");
            }
            else if(splited[0].equals("/modelist") || splited[0].equals("/model")) // kui kontrollib neid asju,
            {
                // Display help for the user, kontrollime, mis commandi kasutaja sisestas
                System.out.println("=================================");
                System.out.println("Available modes:");
                System.out.println("plaintext - set mode to plaintext");
                System.out.println("password \"password\" - set mode password with and use password \"password\"");
                System.out.println("=================================");
            }
            else if(splited[0].equals("/v") || splited[0].equals("/verbose")) //
            {
                // Turn verbose mode on and off, see on nagu valguslüliti, kas on see või ei ole, kui / verbose siis sisse kui enne ei olenud ja vastupidi
                if(verbose){
                    verbose = false;
                    System.out.println("System: Verbose mode turned OFF");
                }else{
                    verbose = true;
                    System.out.println("System: Verbose mode turned ON");
                }
            }
            else if(splited[0].equals("/d") || splited[0].equals("/debug")) //
            {
                System.out.println("DEBUG: User name: "+_username);
                System.out.println("For modeList");
                for(Map.Entry<String, HashMap> entry : modeList.entrySet()) {
                    String key = entry.getKey();
                    HashMap value = entry.getValue();

                    System.out.println("  "+key+" => "+value.get("mode").toString());
                }

                System.out.println("For tempModeSrc");
                for(Map.Entry<String, HashMap> entry : modeList.entrySet()) {
                    String key = entry.getKey();
                    HashMap value = entry.getValue();

                    System.out.println("  "+key+" => "+value.get("mode").toString());
                }

                System.out.println("For tempModeDest");
                for(Map.Entry<String, HashMap> entry : modeList.entrySet()) {
                    String key = entry.getKey();
                    HashMap value = entry.getValue();

                    System.out.println("  "+key+" => "+value.get("mode").toString());
                }
            }
            else if(splited[0].equals("/reg") || splited[0].equals("/register")) // kas registreerib kasutajanime ära
            {
                //Register username  vaatab kas splited array on suure kui 1 või on tühi, kas üldse sisestas kasutajanime
                if(splited.length <= 1 || splited[1].trim().isEmpty()){
                    System.out.println("System: username can't be empty!");
                }
                else{
                    // Create json object to send to server - see määrab süsteemisiseselt kasutajanimeks, mis on splited1
                    _username = splited[1];     // json objekt on pm uus array lihtsalt
                    JsonObject register = new JsonObject();
                    register.addProperty("command","register"); // lisame uue keyga command, läheb serverisse, ütleme command on register
                    register.addProperty("src",_username); //src kasutajanimi

                    output = register.toString(); // jsonobject teha stringiks, mida on võimalik serverile saata

                    if(verbose) System.out.println("System: Add \"register\" to commands");
                    commands.add("register");  // iga kord kui serverisse saadetakse command , lisatakse se Queue-sse
                    // ja kui server annab vastust, siis on teada, millisele commandile see tuli, see on hiljem
                }
            }
            else if(_username.isEmpty()) {
                // Check if username has been set
                System.out.println("System: set username with /register command!");
            }
            else if(splited[0].equals("/list")) { // see on sama nagu registreerimisega, uus jsoni obkjekt, lisatakse command list ja saadetakse enda kasutajale
                // Check who is online
                // Create json object to send to server
                JsonObject listing = new JsonObject();
                listing.addProperty("command","list");
                listing.addProperty("src",_username);
                output = listing.toString();

                if(verbose) System.out.println("System: Add \"list\" to commands"); // kui tahan näha, mis süsteem kogu aeg teeb, saan verbose sisse
                commands.add("list");              //command on queue ja add lisab sinna asju      // lülitada ja ta prindib igat etappi välja
            }
            else if(splited[0].equals("/mode") || splited[0].equals("/setmode")) // kas registreerib kasutajanime ära
            {
                if(splited.length <= 1 || splited[1].trim().isEmpty()){
                    System.out.println("System: username can't be empty!");
                }
                else{
                    String dest = splited[1].trim();
                    if(splited.length <= 2 || splited[2].trim().isEmpty()){
                        System.out.println("System: mode can't be empty! use /modelist to see the mode options");
                    }
                    else if(splited[2].equals("plain") || splited[2].equals("text") || splited[2].equals("plaintext")){
                        if(!modeList.containsKey(dest)){
                            System.out.println("System: no security mode has been set with \""+dest+"\"!");
                        }
                        else{
                            if(tempModeDest.containsKey(dest) && tempModeDest.get(dest).get("mode").equals("plaintext")){
                                modeList.remove(dest);
                                tempModeDest.remove(dest);
                                System.out.println("System: switched modes to \"plaintext\" with user \""+dest+"\"!");
                            }
                            else{
                                //Generate new entry
                                HashMap<String, String> mode = new HashMap<String, String>();
                                mode.put("mode","plaintext");

                                tempModeSrc.put(dest,mode);
                                System.out.println("System: set mode to \"plaintext\" with user \""+dest+"\"! \"\"+dest+\"\" needs to set the same mode before it applys!");
                            }

                            //Send out notification to other person for request/confirmation to change modes
                            JsonObject message = new JsonObject();
                            message.addProperty("command", "send");
                            message.addProperty("src",_username);
                            message.addProperty("mode", "plaintext");
                            output = message.toString();

                            if(verbose) System.out.println("System: Add \"send\" to commands");
                            commands.add("send"); //command on queue ja add lisab sinna asju
                        }
                    }
                    else if(splited[2].equals("pass") || splited[2].equals("password")){
                        if(splited.length <= 3 || splited[3].trim().isEmpty()){
                            System.out.println("System: password can't be empty for \"password\" mode!");
                        }
                        else{
                            String password = String.join(" ", Arrays.copyOfRange(splited, 3, splited.length));

                            if(password.length() > 32){
                                System.out.println("System: password needs to be less than 32 characters long!");
                            }
                            else{
                                try{
                                    String secret = generateKeyFromPassword(password);

                                    //Generate new entry
                                    HashMap<String, String> mode = new HashMap<String, String>();
                                    mode.put("mode","password");
                                    mode.put("key",secret);

                                    if(tempModeDest.containsKey(dest) && tempModeDest.get(dest).get("mode").equals("password")){
                                        //Set the new mode as active
                                        modeList.put(dest,mode);
                                        tempModeDest.remove(dest);
                                        System.out.println("System: Switched modes to \"password\" with user \""+dest+"\"!");
                                    }else{
                                        tempModeSrc.put(dest,mode);
                                        System.out.println("System: Set mode to \"password\" with user \""+dest+"\"! \""+dest+"\" needs to set the same mode before it applys!");
                                    }

                                    //Send out notification to other person for request/confirmation to change modes
                                    JsonObject message = new JsonObject();
                                    message.addProperty("command", "send");
                                    message.addProperty("src",_username);
                                    message.addProperty("mode", "password");
                                    output = message.toString();

                                    if(verbose) System.out.println("System: Add \"send\" to commands");
                                    commands.add("send"); //command on queue ja add lisab sinna asju
                                }
                                catch (Exception e){
                                    System.out.println("System: Unable to generate key for \"password\" mode with password!");
                                    System.out.println(e.toString());
                                }
                            }
                        }
                    }else {
                        System.out.println("System: \""+splited[2]+"\" is not a valid mode! Use /modelist to see the mode options");
                    }
                }
            }
            else if(splited[0].equals("/all")) {            // kui tahad global chatti kirjutada midagi
                // Send message to All
                String messages = String.join(" ", Arrays.copyOfRange(splited, 1, splited.length)); // tühik, funktsiooni alguses splittis tühikute järgi
                // nüüd peab kokku splittima, et saada message

                partner = "";
                if(!messages.isEmpty()) { // json objekt on array  andmete kogum
                    // Create json object to send to server
                    output = createMessage(messages);

                    if(verbose) System.out.println("System: Add \"send\" to commands");
                    commands.add("send"); //command on queue ja add lisab sinna asju
                }
            }
            else{
                System.out.println("System: \""+splited[0]+"\" is not a valid command! Use /help to see commands"); // nüüd kõik need commanded on läbi ja kui on muud sisestatud
                // siis see pole see, mida kasutada
            }
        }
        else if(_username.isEmpty()) {      // kui kasutajanimi on määratud igaks juhuks kontrollib
            // Check if username has been set
            System.out.println("System: set username with /register command!");
        }
        else if(c.equals("@")) { // kas kõige esimene asi on @ märk
            // Send message to specific person
            String messages = String.join(" ", Arrays.copyOfRange(splited, 1, splited.length)); // siis ühendab nagu all'is kõik asjad

            partner = splited[0].substring(1).trim(); // kui pöördusid kellegi poole, siis partner

            if(!messages.isEmpty()) {           // kui on midagi talle saata, siis saada, loob uue objekti , mille ära saadab
                output = createMessage(messages);

                if(verbose) System.out.println("System: Add \"send\" to commands"); //
                commands.add("send");
            }
        }else{
            // Send message to last person
            String messages = String.join(" ", splited); // liidab jälle kõik kokku
            if(!messages.isEmpty()){
                output = createMessage(messages);

                if(verbose) System.out.println("System: Add \"send\" to commands");
                commands.add("send");
            }
        }

        return output; // tagasta jsoni objekt,  mis võibolla luuakse, õieti selle jsoni string
    }

    /*
        Process command from the server and return a responce if needed
        processCommand töötleb serverilt saadud infot ja saadab vajadusel vastuse
    */
    String processResponse(String input){ // kui serverist tuleb mingi vastus, siis see on see funktsioon, mis parseb seda vastust
        // parse on protsess, töötleb, muudab
        String output = null;
        input = input.replace("\n", "").trim(); // tühikud ja enterid võetakse ära

        try{
            // Try to parse json from the server , võtab stringi mis server staatis ja teeb json objektiks , pm arrayks
            JsonObject resp = new JsonParser().parse(input).getAsJsonObject();
            if (resp.isJsonObject()) {
                // Get elements     seal saavad olla erinevad, mis võivad olla seal vastuses, aga ei pruugi
                JsonElement errors = resp.get("error");
                JsonElement result = resp.get("result");
                JsonElement message = resp.get("message");
                JsonElement mode = resp.get("mode");
                JsonElement iv = resp.get("iv");
                JsonElement src = resp.get("src");

                if (src != null) { // st et keegi kirjutas sulle selle, see ei ole tühi vastus
                    // Display message content ja kui sõnumi sisuks on ka midagi, siis edastab selle sõnumi,
                    // siia peab hakkama krüptimise asju  lisama

                    if (message != null) {
                        String mess = null;
                        if(verbose) System.out.println("System: got message from "+src.getAsString()+"!");

                        if(mode != null){
                            if(modeList.containsKey(src.getAsString())){
                                if(verbose) System.out.println("System: message is protected!");
                                HashMap modes = modeList.get(src.getAsString());
                                if(modes.get("mode").equals("password")){
                                    if(iv != null){
                                        try{
                                            mess = decryptWithKey(iv.getAsString(),message.getAsString(),modes.get("key").toString());
                                        }
                                        catch (Exception e){
                                            System.out.println("System: unable to decrypt message from \""+src.getAsString()+"\" using \"password\" mode!");
                                            System.out.println(e.toString());
                                        }
                                    }else{
                                        System.out.println("System:a message from \""+src.getAsString()+"\" using \"password\" mode contained no IV!");
                                    }
                                }
                            }else{
                                System.out.println("System: User \""+src.getAsString()+"\" sent message in \""+mode.getAsString()+"\" mode but are not in that mode!");
                            }
                        }
                        else{
                            mess = message.getAsString();
                        }

                        if(mess != null){
                            // Message from someone, väljundab selle kasutajanime, kes saatis
                            System.out.print("@"+src.getAsString()+": ");
                            System.out.println(mess);
                        }
                    }
                    else if (mode != null) {
                        if(tempModeSrc.containsKey(src.getAsString()) && tempModeSrc.get(src.getAsString()).get("mode").equals(mode.getAsString())){
                            //Set the new mode as active
                            modeList.put(src.getAsString(),tempModeSrc.get(src.getAsString()));
                            tempModeSrc.remove(src.getAsString());

                            System.out.println("System: User \""+src.getAsString()+"\" has switched mode to \""+mode.getAsString()+"\"! This mode is now active!");
                        }else{
                            //Generate new entry
                            HashMap modes = new HashMap<>();
                            modes.put("mode",mode.getAsString());
                            tempModeDest.put(src.getAsString(),modes);

                            System.out.println("System: User \""+src.getAsString()+"\" has put mode as \""+mode.getAsString()+"\"! You need to set your mode to same before it applys!");
                        }
                    }
                }
                if (errors != null) { // kui serveri poolt tulid mingid errorid st mingid vastused, siis
                    // Message from server
                    String error = errors.getAsString().trim(); // error tehakse stringiks
                    String latest = commands.remove().toString(); // command mida lisasime queuesse ja võtame
                    // sealt asja välja, mis sisestati kõige esimesene

                    if(verbose) System.out.println("System: last command was: "+latest); //süsteemi logimine kui tahta sisselülitada

                    if(latest.equals("send")){      //kui kõige esimene command, mis oli queues ja millele ei ole vastust tulnud oli send
                        // If last command was "send", process response
                        if(error.equals("ok")){ // kui error on ok, siis st et sõnum saadeti ilusasti välja
                            // kui logimine on sisselülitatud, siis see tagastatakse ka kasutajale, st server arvas nii
                            if(verbose) System.out.println("System: message delivered");
                        }
                        else if(error.equals("unknown")){ //kasutajanime, kellele taheti saata, ei eksisteeri
                            // sõnum tagastatakse saatjale
                            System.out.println("Server: Wrong username.");
                        }
                        else{
                            System.out.println("Server: Unexpected response for messageing: "+error);
                        }
                    }
                    else if(latest.equals("register")){ // kui registreeriti, siis saab olla kaks vastust
                        // If last command was "register", process response
                        if(error.equals("ok") || error.equals("registered")){
                            if(verbose) System.out.println("Server: Name registerd!"); // kui error on ok ja registered, sis edasi
                        }
                        else if(error.equals("re-registered")){
                            System.out.println("Server: Name changed!");
                        }
                        else{
                            System.out.println("Server: Unexpected response for /register: "+error);
                        }
                    }
                    else if(latest.equals("list")){ // list on command, mis tagastab kõik kasutajad, kes on serveris
                        // If last command was "list", process response
                        if(error.equals("ok")){
                            JsonArray users = result.getAsJsonArray(); //jsoni objektis on sees veel üks array kasutajatest, mille peame kätte saama
                            List userList = new ArrayList();

                            // Check the list returned from server
                            for(JsonElement user : users){ // käime iga elemendi saadud arrays kasutajate list läbi
                                JsonObject usr = user.getAsJsonObject(); // parsame seda , muudame json objektiks, et saame andmeid välja võtta
                                // vaatame, kas nimi on olemas src ja lisame user listi
                                if(usr != null){
                                    userList.add(usr.get("src").getAsString());
                                }
                            }

                            // Check if the list was empty
                            if(userList.isEmpty()){ // seda ei tohiks kunagi juhtuda, kuna oleme alati sees
                                System.out.println("Server: No users are online");
                            }
                            else{
                                // Output the list to user
                                System.out.println("Server: Users online: "+String.join(", ",userList));
                            }
                        }
                        else{ // kui ei tule midagi, siis ..., ei tohiks kunagi juhtuda
                            System.out.println("Server: Unknown response for list: "+error);
                            if (result != null){
                                System.out.println("Result: "+result.toString());
                            }
                        }
                    }
                    else{
                        // Unexpected message from server
                        System.out.println("Server: Unknown response: "+error);
                        if (result != null){
                            System.out.println("Result: "+result.toString());
                        }
                    }
                }
            }
            else{
                // Unable to parse json from server
                System.err.println( "Error while reading command from server: "+input );
            }
        } catch (Exception e) {
            System.err.println( "Error while reading JSON command from server: "+input );
            System.err.println(e);
        }

        return output; // kui server on saatnud meile midagi, mis nõuab kohest vastust, kui kirjutada midagi outputi, siis
        // see saadetakse serverile automaatselt tagasi
    }

    public String createMessage(String messag) {
        if(verbose) System.out.println("System: create message for \""+partner+"\"!");

        JsonObject message = new JsonObject();
        message.addProperty("command","send");
        message.addProperty("src",_username);
        if(!partner.isEmpty()) message.addProperty("dst",partner);

        if(!partner.isEmpty() && modeList.containsKey(partner)){
            if(verbose) System.out.println("System: .. has a mode with "+partner+"!");
            HashMap modes = modeList.get(partner);
            if(modes.get("mode").equals("password")){
                message.addProperty("mode","password");
                try{
                    if(verbose) System.out.println("System: encrypting message with password!");
                    String[] encrypted = encryptWithKey(messag, modes.get("key").toString());
                    message.addProperty("iv",encrypted[0]);
                    messag = encrypted[1];
                }
                catch (Exception e){
                    System.out.println("System: unable to encrypt message using \"password\" mode!");
                    System.out.println(e.toString());
                }
            }
        }

        message.addProperty("message",messag);

        return message.toString();
    }

    public String generateKeyFromPassword(String password) throws Exception {
        if(verbose) System.out.println("System: generateing AES key with password!");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), new byte[16], 65536, 256); // AES-256
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        String secretString = new String(f.generateSecret(spec).getEncoded());

        return secretString;
    }


    public String[] encryptWithKey(String unencryptedString, String key) throws Exception {
        if(verbose) System.out.println("System: encrypting message with aes key!");
        byte[] decodedKey = key.getBytes("UTF8");
        SecretKey secret = new SecretKeySpec(decodedKey, 0, decodedKey.length, passwordCypherScheme);

        String encryptedString = null;
        String iv = null;
        Cipher cipher = Cipher.getInstance(passwordCypherSchemeLong);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] plainText = unencryptedString.getBytes("UTF8");
            byte[] encryptedText = cipher.doFinal(plainText);
            encryptedString = new String(encryptedText);
            iv = new String(cipher.getIV());

        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] output = {iv, encryptedString};
        return output;
    }

    public String decryptWithKey(String ivString, String encryptedString, String key) throws Exception {
        if(verbose) System.out.println("System: decrypting message with aes key!");
        byte[] decodedKey = key.getBytes("UTF8");
        SecretKey secret = new SecretKeySpec(decodedKey, 0, decodedKey.length, passwordCypherScheme);

        byte[] iv = ivString.getBytes("UTF8");

        String decryptedText=null;
        Cipher cipher = Cipher.getInstance(passwordCypherSchemeLong);
        try {
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            byte[] encryptedText = encryptedString.getBytes("UTF8");
            byte[] plainText = cipher.doFinal(encryptedText);
            decryptedText= new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }


}