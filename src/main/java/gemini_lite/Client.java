package gemini_lite;

import java.io.IOException;

import engines.ClientEngine;

public class Client {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Client <URL> [<input>]");
            System.exit(1);
        }

        String url = args[0];
        String input;
        if (args.length == 2) {
            input = args[1];
        }
        else {
            input = null;
        }

        try {
            ClientEngine.run(url, input);
        } 
        catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } 
    }
}
