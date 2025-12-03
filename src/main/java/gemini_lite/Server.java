package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import engines.ServerEngine;

public class Server {
    public static void main(String[] args) {
        System.exit(runServerMain(args));
    }

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

    private static void validateArgs(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Server <directory> [<port>]");
        }
    }
}
