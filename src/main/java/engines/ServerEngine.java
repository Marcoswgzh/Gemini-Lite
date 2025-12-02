package engines;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
    private static Map<String, String> mediaTypes = Map.of("gmi", "text/gemini", "txt", "text/plain");

    public static void run(Socket client, Path directory) {
        try (Socket sock = client; InputStream i = sock.getInputStream(); OutputStream out = sock.getOutputStream()) {
            Request request;
            try {
                request = Request.parse(i);
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

            if (!resolvedPath.startsWith(directory) || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath) || !Files.isReadable(resolvedPath)) {
                sendErrorReply(out, 51, "Not found");
                return;
            }

            String mediaType = determineMediaType(resolvedPath.getFileName().toString());

            try (InputStream j = new BufferedInputStream(Files.newInputStream(resolvedPath))) {
                sendSuccessReply(out, 20, mediaType, j);
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
            e.printStackTrace();
        }
    }

    private static void sendSuccessReply(OutputStream out, int statusCode, String meta, InputStream bodyStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = bodyStream.read(buf)) > 0) {
                buffer.write(buf, 0, n);
            }
            new Reply(statusCode, meta, buffer.toByteArray()).format(out);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String extractPathFromUrl(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd == -1) {
            return null;
        }
        int pathStart = url.indexOf('/', schemeEnd + 3);
        if (pathStart == -1) {
            return "/";
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
