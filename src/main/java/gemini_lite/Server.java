package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import engines.ServerEngine;

/**
 * Gemini-lite server application
 * This class is responsible for:
 * Validating command-line arguments
 * Resolving the server root directory
 * Parsing the optional port number
 * Starting the server socket
 */
public class Server {
    /**
     * Program entry point
     *
     * @param args command-line arguments:
     * args[0]: Root directory to serve files from (required)
     * args[1]: Port number to listen on (optional, 1958 if nothing is passed)
     */
    public static void main(String[] args) {
        System.exit(runServerMain(args));
    }

    /**
     * Main execution of the server
     * This method:
     * Validates the command-line arguments
     * Resolves the serving directory
     * Parses the optional port number
     * Starts the server
     * 
     * @param args: command-line arguments
     * @return 0 on success or 1 on failure
     */
    public static int runServerMain(String[] args) {
        validateArgs(args);

        Path directory = Path.of(args[0]).toAbsolutePath().normalize();
        if (!directory.toFile().isDirectory()) {
            System.err.println("Specified directory does not exist or is not a directory.");
            return 1;
        }

        int port = 1958; 
        if (args.length == 2) {
            try {
                port = Integer.parseInt(args[1]);
            } 
            catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                return 1;
            }
        }

        return startServer(port, directory);
    }

    /**
     * Starts the server listening loop
     * This method:
     * Creates a ServerSocket on the given port 
     * Accepts client connections
     * Delegates each connection to ServerEngine.run(Socket, Path)
     * The server runs indefinitely until the process is terminated
     * @param port: Port number to listen on
     * @param directory: Root directory to serve files from
     * @return process exit code (1 if start fails)
     */
    public static int startServer(int port, Path directory) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Server started on port " + port + ". Serving directory " + directory);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    ServerEngine.run(clientSocket, directory);
                } 
                catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }
        }
        catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            return 1;
        }
    }

     /**
     * Validates the command-line arguments
     *
     * @param args: Command-line arguments
     * @throws IllegalArgumentException if an invalid number of arguments is supplied
     */
    private static void validateArgs(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Server <directory> [<port>]");
        }
    }
}
