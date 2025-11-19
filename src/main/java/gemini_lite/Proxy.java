package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import engines.ProxyEngine;

public class Proxy {
    private int port;

    public Proxy(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Proxy <port>");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
            new Proxy(port).start();
        } 
        catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
        }
        catch (IOException e) {
            System.err.println("Failed to start proxy: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Proxy started on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    clientSocket.setSoTimeout(10000);
                    ProxyEngine.run(clientSocket);
                } 
                catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }
        }
    }
}

