package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import engines.ProxyEngine;

public class Proxy {
    private final int port;

    public Proxy(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        System.exit(runProxyMain(args));
    }

    public static int runProxyMain(String[] args) {
        try {
            validateArgs(args);
            int port = Integer.parseInt(args[0]);
            new Proxy(port).startProxy();
            return 0;
        }
        catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return 1;
        }
        catch (IOException e) {
            System.err.println("Failed to start proxy: " + e.getMessage());
            return 1;
        }
        catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
            return 1;
        }
    }

    public static void validateArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Proxy <port>");
        }
    }

    public void startProxy() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Proxy started on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    clientSocket.setSoTimeout(10000);
                    ProxyEngine.runProxy(clientSocket);
                } 
                catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }
        }
    }
}