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

    public static void parseProxy(String proxyEnv) {
        int colonIndex = proxyEnv.indexOf(':');
        if (colonIndex == -1) {
            System.err.println("Invalid GEMINI_LITE_PROXY format. Expected hostname:port");
            System.exit(1);
        }
        proxyHost = proxyEnv.substring(0, colonIndex);
        try {
            proxyPort = Integer.parseInt(proxyEnv.substring(colonIndex + 1));
        } 
        catch (NumberFormatException e) {
            System.err.println("Invalid GEMINI_LITE_PROXY port: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void run(Socket clientSocket) {
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

            String targetUrl = clientRequest.url;
            String userInput = clientRequest.input;

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

    private static Reply handleRequest(String url, String userInput) {
        Set<String> visited = new HashSet<>();
        int redirectsCount = 0;
        String currentUrl = url;
        String currentInput = userInput;

        while (true) {
            if (visited.contains(currentUrl)) {
                return new Reply(43, "Proxy error: redirect loop detected", null);
            }
            visited.add(currentUrl);

            if (redirectsCount > 10) {
                return new Reply(43, "Proxy error: too many redirects", null);
            }

            Reply reply;
            try {
                reply = sendToServer(currentUrl, currentInput);
            } 
            catch (ProtocolSyntaxException e) {
                return new Reply(43, "Proxy error: origin server protocol error", null);
            } 
            catch (IOException e) {
                return new Reply(43, "Proxy error: unable to connect to origin server", null);
            } 
            catch (Exception e) {
                return new Reply(43, "Proxy error: " + e.getMessage(), null);
            }

            int category = reply.statusCode / 10;

            switch (category) {
                case 1: 
                    return reply;

                case 2: 
                    return reply;

                case 3: 
                    redirectsCount++;
                    currentUrl = reply.meta;
                    currentInput = null; 
                    break;

                case 4: 
                case 5: 
                    return reply;

                case 6: //x6 for slowdowns
                    System.err.println("Origin server slow down: " + reply.meta);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    return new Reply(43, "Proxy error: invalid status code from origin", null);
            }
        }
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
        String hostPart = (slashIndex == -1) ? remainder : remainder.substring(0, slashIndex);
        
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
