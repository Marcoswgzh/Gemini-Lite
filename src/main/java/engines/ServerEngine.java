package engines;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import protocol.ProtocolSyntaxException;
import protocol.Reply;
import protocol.Request;

public class ServerEngine {
    private static final Map<String, String> mediaTypes = Map.of("gmi", "text/gemini", "txt", "text/plain");

    public static void run(Socket client, Path directory) {
        try (Socket socket = client; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            Request request;
            try {
                request = Request.parse(in);
            } 
            catch (ProtocolSyntaxException e) {
                sendErrorReply(out, 59, "Bad request syntax");
                return;
            } 
            catch (IOException e) {
                return; 
            }

            String url = request.getUrl();
            String pathStr = extractPathFromUrl(url);
            if (pathStr == null) {
                sendErrorReply(out, 59, "Malformed request URL");
                return;
            }

            Path resolvedPath = directory.resolve("." + pathStr).normalize();

            if (!resolvedPath.startsWith(directory)) {
                sendErrorReply(out, 59, "Invalid path");
                return;
            }

            if (!Files.exists(resolvedPath) || Files.isDirectory(resolvedPath) || !Files.isReadable(resolvedPath)) {
                sendErrorReply(out, 51, "Not found");
                return;
            }

            String mediaType = determineMediaType(resolvedPath.getFileName().toString());

            try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(resolvedPath))) {
                byte[] fileData = fileIn.readAllBytes();
                Reply reply = new Reply( 20, mediaType, fileData);
                reply.format(out);
            } 
            catch (IOException e) {
                sendErrorReply(out, 50, "Server error");
            }

        } 
        catch (Exception e) {
            System.err.println("Unexpected error handling client: " + e.getMessage());
        }
    }

    private static void sendErrorReply(OutputStream out, int statusCode, String meta) {
        try {
            new Reply(statusCode, meta, null).format(out);
        } 
        catch (IOException e) {
            System.err.println("Failed to send error reply: " + e.getMessage());
        }
    }

    private static String extractPathFromUrl(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd == -1) {
            return null;
        }
        int authorityStart = schemeEnd + 3;
        int pathStart = url.indexOf('/', authorityStart);

        if (pathStart == -1) {
            pathStart = url.length();
        }

        String authority = url.substring(authorityStart, pathStart);
        if (authority.contains("@")) {
            return null;
        }

        int queryStart = url.indexOf('?', pathStart);
        if (queryStart == -1) {
            queryStart = url.length();
        }
        
        return url.substring(pathStart, queryStart);
    }

    private static String determineMediaType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return mediaTypes.getOrDefault(extension, "application/octet-stream");
    }
}
