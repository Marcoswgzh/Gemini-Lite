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

/**
 * Core request handler for the Gemini-Lite server
 * This class is responsible for:
 * Parsing incoming client requests
 * Validating and resolving filesystem paths 
 * Determining media (MIME) types
 * Serving files from a root directory
 * Sending appropriate protocol error replies
 */
public class ServerEngine {
    /** Mapping of file extensions to media (MIME) types */
    private static final Map<String, String> mediaTypes = Map.of("gmi", "text/gemini", "txt", "text/plain");

    /**
     * Handles client connection
     * This method:
     * Parses the incoming Request
     * Extracts and validates the requested path
     * Reads and serves the requested file
     * Sends appropriate error replies on failure
     * 
     * @param client: Connected client socket
     * @param directory: Server root directory used to resolve file paths
     */
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

    /**
     * Sends error reply to the client
     *
     * @param out: Client output stream
     * @param statusCode: Protocol status code
     * @param meta: Error message
     */
    private static void sendErrorReply(OutputStream out, int statusCode, String meta) {
        try {
            new Reply(statusCode, meta, null).format(out);
        } 
        catch (IOException e) {
            System.err.println("Failed to send error reply: " + e.getMessage());
        }
    }

    /**
     * Extracts the path from a URL
     * This method enforces that:
     * A valid :// scheme separator must exist
     * User info user@host is not allowed
     * Fragments # are rejected
     * Query strings ? are stripped
     * @param url: Request URL
     * @return the extracted path, or null if the URL is malformed
     */
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

        int fragmentIndex = url.indexOf('#', pathStart);
            if (fragmentIndex != -1) {
                return null; 
        }

        return url.substring(pathStart, queryStart);
    }

    /**
     * Determines the media (MIME) type for a file based on its extension
     * 
     * @param fileName: Name of the file
     * @return the resolved MIME type, or application/octet-stream if unknown
     */
    private static String determineMediaType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return mediaTypes.getOrDefault(extension, "application/octet-stream");
    }
}
