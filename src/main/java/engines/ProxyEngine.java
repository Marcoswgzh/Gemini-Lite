package engines;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import protocol.ProtocolSyntaxException;
import protocol.Reply;
import protocol.Request;

/**
 * Acts as a proxy server for the Gemini-Lite protocol
 * This class is responsible for:
 * Parsing proxy configuration
 * Accepts client connections
 * Forwards requests to upstream server
 * Handles redirects and slowdown responses
 * Relays final replies back to the client
 */
public class ProxyEngine {
    /** Proxy target host */
    public static String proxyHost;

    /** Proxy target port */
    public static int proxyPort;

    /** Indicates whether proxy mode is enabled */
    public static boolean useProxy = false;

    /** Maximum number of redirects allowed before aborting */
    private static final int max_redirects = 5;

    /** Maximum number of slowdown retries before aborting */
    private static final int max_slowdown_retries = 5;

    /**
     * Parses proxy configuration from an environment variable string
     * @param proxyEnv: Proxy environment variable
     */
    public static void parseProxy(String proxyEnv) {
        int colonIndex = proxyEnv.indexOf(':');
        if (colonIndex == -1) {
            System.err.println("Invalid proxy format. Expected hostname:port");
            System.exit(1);
        }
        proxyHost = proxyEnv.substring(0, colonIndex);
        try {
            proxyPort = Integer.parseInt(proxyEnv.substring(colonIndex + 1));
        } 
        catch (NumberFormatException e) {
            System.err.println("Invalid proxy port: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Handles proxy client connection
     * This method:
     * Parses the incoming client request
     * Forwards it to the target server
     * Processes redirects and retry responses
     * Sends the final reply back to the client
     * 
     * @param clientSocket: Accepted client socket
     */
    public static void runProxy(Socket clientSocket) {
        try (Socket sock = clientSocket;
            InputStream clientIn = sock.getInputStream();
            OutputStream clientOut = sock.getOutputStream()) {

            Request clientRequest;
            try {
                clientRequest = Request.parse(clientIn);
            } 
            catch (ProtocolSyntaxException e) {
                sendReply(clientOut, 59, "Bad request");
                return;
            } 
            catch (IOException e) {
                return;
            }

            String targetUrl = clientRequest.getUrl();
            String userInput = clientRequest.getInput();

            Reply finalReply = handleRequest(targetUrl, userInput);

            try {
                finalReply.format(clientOut);
            } 
            catch (IOException e) {
                System.err.println("Error sending reply to client: " + e.getMessage());
            }

        } 
        catch (Exception e) {
            System.err.println("Unexpected error in proxy: " + e.getMessage());
        }
    }

    /**
     * Handles forwarding logic for a proxied request
     * This method continuously processes protocol responses until:
     * A successful response is received
     * A fatal error occurs
     * The redirect limit is exceeded
     * A loop is detected
     * 
     * @param url: Initial target URL
     * @param userInput: optional user input
     * @return the final Reply to be returned to the client
     * @throws IOException if a fatal I/O error occurs
     */
    private static Reply handleRequest(String url, String userInput) throws IOException {
        Set<String> visited = new HashSet<>();
        int redirectsCount = 0;
        String currentUrl = url;
        String currentInput = userInput;
        int retries = 0;

        while (true) {
            if (visited.contains(currentUrl)) {
                return new Reply(43, "Proxy error: redirect loop detected", null);
            }
            visited.add(currentUrl);

            Reply reply;
            try {
                reply = sendToServer(currentUrl, currentInput);
            } 
            catch (ProtocolSyntaxException e) {
                return new Reply(43, "Proxy error: server protocol error", null);
            } 
            catch (IOException e) {
                return new Reply(43, "Proxy error: unable to connect to server", null);
            } 
            catch (Exception e) {
                return new Reply(43, "Proxy error: " + e.getMessage(), null);
            }
            int statusCode = reply.getStatusCode();
            int category = statusCode / 10;

            switch (category) {
                case 1: 
                    return reply;

                case 2: 
                    return reply;

                case 3: 
                    redirectsCount++;
                    if (redirectsCount > max_redirects) {
                        return new Reply(43, "Proxy error: too many redirects", null);
                    }
                    currentUrl = reply.getMeta();
                    currentInput = null; 
                    break;

                case 4: 
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

                case 5: 
                    return reply;

                default:
                    return new Reply(43, "Proxy error: invalid status code", null);
            }
        }
    }

    /**
     * Handles exponential slowdown for origin server responses
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
     * Sends a request to the origin server and retrieves the reply
     * 
     * @param url: Request URL
     * @param userInput: Optional user input
     * @return the parsed Reply
     * @throws IOException if a network error occurs
     * @throws ProtocolSyntaxException if the URL format is invalid
     */
    private static Reply sendToServer(String url, String userInput) throws IOException, ProtocolSyntaxException {
        String urlStr = url;
        if (!urlStr.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Invalid URL scheme");
        }

        String remainder = urlStr.substring("gemini-lite://".length());
        String host;
        int port = 1958;
        
        int slashIndex = remainder.indexOf('/');
        String hostPart;
        if (slashIndex == -1) {
            hostPart = remainder;
        } 
        else {
            hostPart = remainder.substring(0, slashIndex);
        }
        
        int colonIndex = hostPart.indexOf(':');
        if (colonIndex != -1) {
            host = hostPart.substring(0, colonIndex);
            port = Integer.parseInt(hostPart.substring(colonIndex + 1));
        } 
        else {
            host = hostPart;
        }

        try (Socket originSocket = new Socket(host, port)) {
            originSocket.setSoTimeout(15000);
            OutputStream originOut = originSocket.getOutputStream();
            InputStream originIn = originSocket.getInputStream();

            Request request = new Request(url, userInput);
            request.format(originOut);

            Reply reply = Reply.parse(originIn);
            return reply;
        }
    }

    /**
     * Sends reply to a client
     * @param out: Output stream to write to
     * @param statusCode: Reply status code
     * @param meta: Reply metadata
     */
    private static void sendReply(OutputStream out, int statusCode, String meta) {
        try {
            new Reply(statusCode, meta, null).format(out);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
