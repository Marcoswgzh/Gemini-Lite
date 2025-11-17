package gemini_lite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import engines.ServerEngine;

public class Server {
    private final Path directory;
    private final int port;

    public Server(Path directory, int port) {
        this.directory = directory;
        this.port = port;
    }
    
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -cp target/bcs2110-2025.jar gemini_lite.Server <directory> [<port>]");
            System.exit(1);
        }

        Path directory = Path.of(args[0]).toAbsolutePath().normalize();
        if (!directory.toFile().isDirectory()) {
            System.err.println("Specified directory does not exist or is not a directory.");
            System.exit(1);
        }

        int port = 1958; 
        if (args.length == 2) {
            try {
                port = Integer.parseInt(args[1]);
            } 
            catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }

        try {
            new Server(directory, port).start();
        } 
        catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("Server started on port " + port + " serving directory " + directory);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    ServerEngine.run(clientSocket, directory);
                } 
                catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }
        }
    }


}
