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
    private static final int max_redirects = 5;
    private static final int max_slowdown_retries = 5;

    public static void runEngine(String url, String inputArg) throws IOException, ProtocolSyntaxException {
        String currentUrl = url;
        String userInput = inputArg;
        int redirectsCount = 0;

         while (true) {
            Request request = new Request(currentUrl, userInput);
            Reply reply = sendRequest(request);

            int statusCode = reply.getStatusCode();
            int category = statusCode / 10;

            if (statusCode < 10 || statusCode > 69) {
                System.err.println("Out of range status code: " + statusCode);
                System.exit(1); 
            }

            switch (category) {
                case 1: // Informational / Input requested
                    if (userInput == null) {
                        userInput = promptUser(reply.getMeta());
                    }

                    String encodedInput = userInput.replace(" ", "%20");

                    currentUrl = dropQueryString(currentUrl) + "?" + encodedInput;
                    
                    userInput = null; 
                    break;

                case 2: // Success
                    if (reply.getMeta() == null || reply.getMeta().isEmpty()) {
                        System.err.println("Success reply missing mimetype");
                        System.exit(1); 
                    }
                    if (reply.getBody() != null) {
                        System.out.write(reply.getBody());
                        System.out.flush();
                    }
                    return;

                case 3: // Redirection
                    redirectsCount++;
                    if (redirectsCount > max_redirects) {
                        throw new IOException("Too many redirects");
                    }
                    
                    String newUrl;
                    if (reply.getMeta().startsWith("/")) {
                        String schemeAndHost = currentUrl.substring(0, currentUrl.indexOf("/", "gemini-lite://".length()));
                        newUrl = schemeAndHost + reply.getMeta();
                    }
                    else {
                        newUrl = reply.getMeta();
                    }
                    
                    currentUrl = dropQueryString(newUrl);
                    userInput = null;
                    break;

                case 4: // Temporary failure
                    if (statusCode == 44) { 
                        System.err.println("Server asks to slow down: " + reply.getMeta());
                        handleSlowdown();
                    } 
                    else {
                        System.err.println("Temporary failure (" + statusCode + "): " + reply.getMeta());
                        System.exit(statusCode);
                    }
                    break;
                case 5: // Permanent failure
                    System.err.println("Server error (" + statusCode + "): " + reply.getMeta());
                    System.exit(statusCode); 

                default:
                    throw new IOException("Unexpected status code: " + statusCode);
            }
        }
    }

    public static String dropQueryString(String url) {
        int qIndex = url.indexOf('?');
        if (qIndex != -1) {
            return url.substring(0, qIndex);
        }
        return url;
    }

    public static void handleSlowdown() throws IOException {
        int retries = 0;
        while (retries < max_slowdown_retries) {
            try {
                Thread.sleep(1000 * (retries + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during slowdown wait " + e.getMessage());
            }
            retries++;
        }
        throw new IOException("Exceeded maximum slowdown retries");
    }

    public static Reply sendRequest(Request request) throws ProtocolSyntaxException, IOException {
        String urlStr = request.getUrl();
        if (!urlStr.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Error: Request does not begin with \\\"gemini-lite://\\\"");
        }

        String host;
        int port;
        if (ProxyEngine.useProxy) {
            host = ProxyEngine.proxyHost;
            port = ProxyEngine.proxyPort;
        }
        else {
            String remainder = urlStr.substring("gemini-lite://".length());
            String hostPart;
            int slashIndex = remainder.indexOf('/');
            if (slashIndex == -1) {
                hostPart = remainder;
            } 
            else {
                hostPart = remainder.substring(0, slashIndex);
            }

            int colonIndex = hostPart.indexOf(':');
            if (colonIndex != -1) {
                host = hostPart.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPart.substring(colonIndex + 1));
                } 
                catch (NumberFormatException e) {
                    throw new ProtocolSyntaxException("Invalid port in URL: " + hostPart);
                }
            } 
            else {
                host = hostPart;
                port = 1958;
            }

            if (host.isBlank()) {
                throw new ProtocolSyntaxException("Host cannot be empty");
            }
        }

        try (Socket sock = new Socket(host, port)) {
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();
            request.format(out); 
            return Reply.parse(in); 
        }
    }

    public static String promptUser(String prompt) throws IOException {
        Console console = System.console();
        if (console == null) {
            throw new IOException("No console available for user input");
        }
        return console.readLine(prompt + " ");
    }
}
