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
        String userInput = null;
        int redirectsCount = 0;

         while (true) {
            Request request = new Request(currentUrl, userInput);
            Reply reply = sendRequest(request);

            int statusCode = reply.getStatusCode();

            int category = statusCode / 10;

            switch (category) {
                case 1: // Informational / Input requested
                    if (userInput == null) {
                        userInput = promptUser(reply.getMeta());
                    }
                    break;

                case 2: // Success
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
                    currentUrl = reply.getMeta();
                    if (!currentUrl.startsWith("gemini-lite://")) {
                        throw new ProtocolSyntaxException("Redirect URL invalid: " + currentUrl);
                    }
                    userInput = null;
                    break;

                case 4: // Temporary failure
                case 5: // Permanent failure
                    throw new IOException("Server error (" + statusCode + "): " + reply.getMeta());

                case 6: // Slow down
                    handleSlowdown(reply.getMeta());
                    break;

                default:
                    throw new IOException("Unexpected status code: " + statusCode);
            }
        }
    }

    private static void handleSlowdown(String meta) throws IOException {
        int retries = 0;
        while (retries < max_slowdown_retries) {
            System.err.println("Server asks to slow down: " + meta + " (retry " + (retries + 1) + ")");
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

    private static String promptUser(String prompt) throws IOException {
        Console console = System.console();
        if (console == null) {
            throw new IOException("No console available for user input");
        }
        return console.readLine(prompt + " ");
    }
}
