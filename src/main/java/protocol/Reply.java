package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents a reply in the Gemini-Lite protocol.
 * A reply consists of a numeric status code, a metadata string and an optional body 
 */
public class Reply {
    /** Numeric status code of the reply */
    private final int statusCode;

    /** Metadata string */
    private final String meta;

    /** Optional reply body */
    private final byte[] body;

    /**
     * Constructs a new Reply with the given status code, metadata, and body
     * 
     * @param statusCode: Numeric status code
     * @param meta: Metadata string
     * @param body: Optional reply body
     */
    public Reply(int statusCode, String meta, byte[] body) {
        this.statusCode = statusCode;
        this.meta = meta;
        this.body = body;
    }

    /**
     * Returns status code
     * 
     * @return status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns metadata
     * 
     * @return metadata
     */
    public String getMeta() {
        return meta;
    }

    /**
     * Returns body
     * 
     * @return body
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Parses a reply from the given input stream
     * The request line must be terminated by CRLF (\r\n) and contain:
     * A numeric status code
     * A metadata string
     * The remainder of the stream is interpreted as the reply body
     * 
     * @param in: Input stream to read from
     * @return the parsed Reply object
     * @throws ProtocolSyntaxException if the reply is syntactically invalid
     * @throws IOException if an I/O error occurs while reading from the stream
     */
    public static Reply parse(InputStream in) throws ProtocolSyntaxException, IOException {
        String line = readLineCRLF(in);
        checkSyntax(line);

        int spaceIndex = line.indexOf(' ');
        int status;
        String meta;

        if (spaceIndex >= 0) {
            status = Integer.parseInt(line.substring(0, spaceIndex));
            meta = line.substring(spaceIndex + 1).trim();
        } 
        else {
            status = Integer.parseInt(line);
            meta = ""; 
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
        }

        return new Reply(status, meta, buf.toByteArray());
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
     * Checks the syntax of a reply header line:
     * Status code must be numeric
     * Metadata must not exceed 1024 bytes
     *
     * @param line: Reply header line to validate
     * @throws ProtocolSyntaxException if the reply violates protocol rules
     */
    private static void checkSyntax(String line) throws ProtocolSyntaxException {
        int spaceIndex = line.indexOf(' ');

        String statusStr;
        if (spaceIndex >= 0) {
            statusStr = line.substring(0, spaceIndex);
        } 
        else {
            statusStr = line;
        }

        try {
            int status = Integer.parseInt(statusStr);
        } 
        catch (NumberFormatException e) {
            throw new ProtocolSyntaxException("Invalid numeric status code");
        }

        String meta;
        if (spaceIndex >= 0) {
            meta = line.substring(spaceIndex + 1);
        } 
        else {
            meta = "";  
        }

        if (meta.getBytes(StandardCharsets.UTF_8).length > 1024) {
            throw new ProtocolSyntaxException("Reply line too long (>1024)");
        }
    }
       
    /**
     * Writes this reply to the given output stream using CRLF line termination
     * The formatted reply consists of:
     * The status code
     * A space and metadata string
     * A terminating CRLF sequence
     * Optional body data
     * 
     * @param out: Output stream to write to
     * @throws IOException if an I/O error occurs during writing
     */
    public void format(OutputStream out) throws IOException {
        String header = statusCode + " " + meta + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        if (body != null) {
            out.write(body);
        }
        out.flush();
    }
}