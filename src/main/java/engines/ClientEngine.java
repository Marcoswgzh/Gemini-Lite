package engines;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import protocol.ProtocolSyntaxException;
import protocol.Reply;
import protocol.Request;

public class ClientEngine {
    public static void run(String url, String inputArg) throws IOException, ProtocolSyntaxException {
        String currentUrl = url;
        String userInput = null;
        
        boolean inputFromArg = inputArg != null;

        boolean haveUserInput = false;
        int redirects = 0;

        while (true) {
            Request request;

            if (inputFromArg) {
                request = new Request(currentUrl, inputArg);
            } 
            else {
                request = new Request(currentUrl, userInput);
            }

            Reply reply = sendRequest(request);

            int category = reply.statusCode / 10;

            switch (category) {
                case 1: 
                    if (inputFromArg) {
                        userInput = inputArg;
                        haveUserInput = true;
                    } else if (!haveUserInput) {                       
                        userInput = promptUser(reply.meta);
                        haveUserInput = true;
                    }
                    break;

                case 2: 
                    if (reply.body != null) {
                        System.out.write(reply.body);
                        System.out.flush();
                    }
                    System.exit(0);
                    return;

                case 3: 
                    redirects++;
                    if (redirects > 10) {
                        System.err.println("Too many redirects");
                        System.exit(1);
                    }
                    currentUrl = reply.meta;
    
                    userInput = null;
                    haveUserInput = false;
                    inputFromArg = false;
                    break;

                case 4: 
                case 5: 
                    System.err.println("Error (" + reply.statusCode + "): " + reply.meta);
                    System.exit(reply.statusCode);
                    return;

                case 6: //x6 for slowdowns
                    System.err.println("Server asks to slow down: " + reply.meta);
                    try {
                        Thread.sleep(1000);
                    } 
                    catch (InterruptedException e) {}
                    break;

                default:
                    System.err.println("Unexpected status code: " + reply.statusCode);
                    System.exit(1);
            }
        }
    }

    public static Reply sendRequest(Request request) throws ProtocolSyntaxException, IOException {
        String urlStr = request.url;
        if (!urlStr.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("URL must start with gemini-lite://");
        }

        String remainder = urlStr.substring("gemini-lite://".length());
        String host;
        int port = 1958; 
        String path = "/";
        int slashIndex = remainder.indexOf('/');
        if (slashIndex == -1) {
            host = remainder;
        } 
        else {
            host = remainder.substring(0, slashIndex);
            path = remainder.substring(slashIndex); 
        }

        int colonIndex = host.indexOf(':');
        if (colonIndex != -1) {
            port = Integer.parseInt(host.substring(colonIndex + 1));
            host = host.substring(0, colonIndex);
        }

        try (Socket sock = new Socket(host, port)) {
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();
            request.format(out); 
            return Reply.parse(in); 
        }
    }

    private static String promptUser(String prompt) {
        Console console = System.console();
        if (console == null) {
            System.err.println("No console available for input.");
            System.exit(1);
        }
        return console.readLine(prompt + " ");
    }
}
