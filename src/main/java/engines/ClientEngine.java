package engines;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import protocol.ProtocolSyntaxException;
import protocol.Reply;
import protocol.Request;

/**
 * Client execution engine for the Gemini-Lite protocol
 * This class is responsible for:
 * Sending requests to servers
 * Handling all protocol status categories
 */
public class ClientEngine {
    /** Maximum number of redirects allowed before aborting */
    private static final int max_redirects = 5;

    /** Maximum number of slowdown retries before aborting */
    private static final int max_slowdown_retries = 5;

    /**
     * Executes the client engine starting from the given URL
     * This method continuously processes protocol responses until:
     * A successful response is received
     * A fatal error occurs
     * The redirect limit is exceeded
     * 
     * @param url: Initial request URL
     * @param inputArg: Initial input, or null if none is provided
     * @param defaultInput: Default input used if user input is required and none is provided
     * @throws IOException if I/O, or protocol error occurs
     * @throws ProtocolSyntaxException if request or reply syntax is invalid
     */
    public static void runEngine(String url, String inputArg, String defaultInput) throws IOException, ProtocolSyntaxException {
        String currentUrl = url;
        String userInput = inputArg;
        int redirectsCount = 0;
        int retries = 0;

        while (true) {
            Request request = new Request(currentUrl, userInput);
            Reply reply = sendRequest(request);

            int statusCode = reply.getStatusCode();
            int category = statusCode / 10;

            if (statusCode < 10 || statusCode > 59) {
                System.err.println("Out of range status code: " + statusCode);
                System.exit(1); 
            }

            switch (category) {
                case 1: // Input expected
                    if (userInput == null) {
                        if (defaultInput != null) {
                            userInput = defaultInput;
                        }
                        else {
                            userInput = promptUser(reply.getMeta());
                        }
                    }

                    String encodedInput = URLEncoder.encode(userInput, StandardCharsets.UTF_8);

                    currentUrl = dropQueryString(currentUrl) + "?" + encodedInput;
                    
                    userInput = null; 
                    defaultInput = null;
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
                        String base;
                        int slash = currentUrl.indexOf("/", "gemini-lite://".length());
                        if (slash == -1) {
                            base = currentUrl;
                        } 
                        else {
                            base = currentUrl.substring(0, slash);
                        }
                        newUrl = base + reply.getMeta();
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
                        handleSlowdown(retries);
                        retries++;
                        continue;
                    } 
                    else {
                        System.err.println("Temporary failure (" + statusCode + "): " + reply.getMeta());
                        System.exit(statusCode);
                    }

                case 5: // Permanent failure
                    System.err.println("Server error (" + statusCode + "): " + reply.getMeta());
                    throw new IOException("Temporary failure " + statusCode); 

                default:
                    throw new IOException("Unexpected status code: " + statusCode);
            }
        }
    }

    /**
     * Removes the query string and fragment from a URL
     * 
     * @param url: Original URL
     * @return URL without query parameters or fragment identifiers
     */
    private static String dropQueryString(String url) {
        int qIndex = url.indexOf('?');
        int fIndex = url.indexOf('#');

        int cut;

        if (qIndex == -1 && fIndex == -1) {
            return url;
        } 
        else if (qIndex == -1) {
            cut = fIndex;
        } 
        else if (fIndex == -1) {
            cut = qIndex;
        } 
        else {
            cut = Math.min(qIndex, fIndex);
        }

        return url.substring(0, cut);
    }

    /**
     * Handles exponential slowdown for server responses
     * 
     * @param retry: the current retry attempt number
     * @throws IOException if the maximum retry limit is exceeded
     */
    private static void handleSlowdown(int retry) throws IOException {
        int delay = 1000 * (int)Math.pow(2, retry);
        try {
            Thread.sleep(delay);
        } 
        catch (InterruptedException e) {
            if (retry > max_slowdown_retries) {
                Thread.currentThread().interrupt();
                throw new IOException("Exceeded maximum slowdown retries");
            }
        }
    }

    /**
     * Sends a request to the target server
     * This method:
     * Extracts host and port from the URL
     * Supports proxy routing
     * Opens TCP socket
     * Sends formatted request
     * Parses the reply
     * 
     * @param request: Request to be sent
     * @return the parsed Reply
     * @throws ProtocolSyntaxException if URL or reply syntax is invalid
     * @throws IOException if a network I/O error occurs
     */
    private static Reply sendRequest(Request request) throws ProtocolSyntaxException, IOException {
        String urlStr = dropQueryString(request.getUrl());
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

    /**
     * Prompts the user for input using the system console
     * 
     * @param prompt: Prompt message displayed to the user
     * @return the user's input
     * @throws IOException if no console is available
     */
    private static String promptUser(String prompt) throws IOException {
        Console console = System.console();
        if (console != null) {
                return console.readLine(prompt + " ");
        } 
        else {
                throw new IOException("No console available for user input");
        }
    }
}