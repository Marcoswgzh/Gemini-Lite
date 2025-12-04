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

public class ProxyEngine {
    public static String proxyHost;
    public static int proxyPort;
    public static boolean useProxy = false;
    private static final int max_redirects = 5;
    private static final int max_slowdown_retries = 5;

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

    private static Reply handleRequest(String url, String userInput) throws IOException {
        Set<String> visited = new HashSet<>();
        int redirectsCount = 0;
        String currentUrl = url;
        String currentInput = userInput;

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
                    if (reply.getStatusCode() == 44) {
                        System.err.println("Server asks to slow down: " + reply.getMeta());
                        handleSlowdown(); 
                    }
                    else {
                        System.err.println("Temporary failure (" + statusCode + "): " + reply.getMeta());
                        System.exit(statusCode);
                    }
                    break;
                case 5: 
                    return reply;

                default:
                    return new Reply(43, "Proxy error: invalid status code", null);
            }
        }
    }

    public static void handleSlowdown() throws IOException {
        int retries = 0;
        int delay = 1000;
        while (retries < max_slowdown_retries) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during slowdown wait " + e.getMessage());
            }
            delay *= 2;
            retries++;
        }
        throw new IOException("Exceeded maximum slowdown retries");
    }

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

    private static void sendReply(OutputStream out, int statusCode, String meta) {
        try {
            new Reply(statusCode, meta, null).format(out);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
