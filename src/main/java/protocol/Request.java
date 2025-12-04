package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents a parsed request in the Gemini-Lite protocol
 * A request consists of a mandatory URL and an optional input string
 */
public class Request {
    /** Requested URL */
    private final String url;

    /** Optional input */
    private final String input;

    /** Contructs a new Request with the given URL and an optional input
     * @param url: Requested URL
     * @param input: Optional input
     */
    public Request(String url, String input) {
        this.url = url;
        this.input = input;
    }

    /** Returns the URL 
     * 
     * @return requested URL
     */
    public String getUrl() {
        return url;
    }

    /** Returns the optional input 
     * 
     * @return optional input
     */
    public String getInput() {
        return input;
    }

    /**
     * Parses a request from the given input stream
     * The request line must be terminated by CRLF (\r\n) and follow the Gemini-Lite protocol syntax. 
     * If a space is present, the portion after the first space is treated as input.
     * 
     * @param in: Input stream to read from
     * @return the parsed request object
     * @throws ProtocolSyntaxException if the request is syntactically invalid
     * @throws IOException if an I/O error occurs while reading from the stream
     */
    public static Request parse(InputStream in) throws ProtocolSyntaxException, IOException {
        String line = readLineCRLF(in);
        checkSyntax(line);

        String url;
        String input = null;
        int spaceIndex = line.indexOf(" ");

        if (spaceIndex >= 0) {
            url = line.substring(0, spaceIndex);
            input = line.substring(spaceIndex + 1).trim();
        } 
        else {
            url = line;
        }
        
        return new Request(url, input);
    } 
    
    /**
     * Reads a CRLF-terminated line from an input stream.
     * The method enforces correct CRLF usage and rejects:
     * End-of-stream before CRLF is found
     * Line feeds (\n) not preceded by a carriage return (\r)
     * 
     * @param in: Input stream to read from
     * @return the line without CRLF
     * @throws IOException if an I/O error occurs
     * @throws ProtocolSyntaxException if CRLF formatting is invalid
     */
    private static String readLineCRLF(InputStream in) throws IOException, ProtocolSyntaxException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int prev = -1;
        
        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new ProtocolSyntaxException("Unexpected EOF (CRLF not found)");
            }

            if (prev == '\r' && b == '\n') {
                break; 
            }

            if (b == '\n' && prev != '\r') {
            throw new ProtocolSyntaxException("Illegal LF without preceding CR");
            }

            if (b != '\r') {
                buf.write(b);
            }

            prev = b;
        }

        return buf.toString(StandardCharsets.UTF_8);
    }

    /**
     * Checks the syntax of a request line:
     * Maximum length of 1024 bytes
     * Must start with "gemini-lite://"
     * 
     * @param line: the request line to validate
     * @throws ProtocolSyntaxException if the request violates syntax rules
     */
    private static void checkSyntax(String line) throws ProtocolSyntaxException {
        if (line.getBytes(StandardCharsets.UTF_8).length > 1024) {
            throw new ProtocolSyntaxException("Request line too long (>1024)");
        }
        
        if (!line.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Request does not begin with \"gemini-lite://\"");
        }
    }

    /**
     * Writes this request to the given output stream using CRLF line termination.
     * The formatted request consists of:
     * The URL
     * An optional space and input
     * A terminating CRLF sequence
     * 
     * @param out: Output stream to write to
     * @throws IOException if an I/O error occurs during writing
     */
    public void format(OutputStream out) throws IOException {
        String line = url;
        if (input != null) {
            line += " " + input;
        }
        line += "\r\n";

        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}