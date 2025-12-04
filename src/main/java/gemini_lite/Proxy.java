package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import engines.ProxyEngine;

/**
 * Gemini-lite proxy application
 * This class is responsible for:
 * Parsing command-line arguments
 * Starting a TCP server socket
 * Accepting incoming client connections
 */
public class Proxy {
    /** Port on which the proxy server listens */
    private final int port;

    /**
     * Constructs a new Proxy instance bound to the given port
     *
     * @param port: Port to listen on
     */
    public Proxy(int port) {
        this.port = port;
    }

    /**
     * Program entry point
     * 
     * @param args: command-line arguments:
     * args[0]: Port number to listen on
     */
    public static void main(String[] args) {
        System.exit(runProxyMain(args));
    }

    /**
     * Executes the main proxy and returns a process exit code
     * This method:
     * Validates command-line arguments
     * Parses the port number
     * Starts the proxy server
     * 
     * @param args: command-line arguments
     * @return 0 on success or 1 on failure
     */
    private static int runProxyMain(String[] args) {
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

    /**
     * Validates the command-line arguments for the proxy
     * Exactly one argument is required: port number to listen on
     * 
     * @param args: command-line arguments to validate
     * @throws IllegalArgumentException if the argument count is invalid
     */
    private static void validateArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Proxy <port>");
        }
    }

    /**
     * Starts the proxy server and begins accepting client connections
     * Creates a ServerSocket bound to the configured port
     * Accepts incoming client connections
     * Delegates each connection to ProxyEngine.runProxy(Socket)
     * 
     * @throws IOException if the server socket cannot be created or bound
     */
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