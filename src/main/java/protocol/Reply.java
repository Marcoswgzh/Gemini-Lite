package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Reply {
    private final int statusCode;
    private final String meta;
    private final byte[] body;

    public Reply(int statusCode, String meta, byte[] body) {
        this.statusCode = statusCode;
        this.meta = meta;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMeta() {
        return meta;
    }

    public byte[] getBody() {
        return body;
    }

    public static Reply parse(InputStream in) throws ProtocolSyntaxException, IOException {
        String line = readLineCRLF(in);
        checkSyntax(line);

        int spaceIndex = line.indexOf(' ');
        int status = Integer.parseInt(line.substring(0, spaceIndex));
        String meta;

        if (spaceIndex >= 0) {
            meta = line.substring(spaceIndex + 1).trim();
        } 
        else {
            meta = ""; 
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
        }

        return new Reply(status, meta, buf.toByteArray());
    }

    public static String readLineCRLF(InputStream in) throws IOException, ProtocolSyntaxException {
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

            if (b != '\r') {
                buf.write(b);
            }

            prev = b;
        }

        return buf.toString(StandardCharsets.UTF_8);
    }

    private static void checkSyntax(String line) throws ProtocolSyntaxException {
        int spaceIndex = line.indexOf(' ');

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

        if (spaceIndex == -1) {
            throw new ProtocolSyntaxException("Missing space between status and meta");
        }

        String statusStr;
        if (spaceIndex >= 0) {
            statusStr = line.substring(0, spaceIndex);
        } 
        else {
            statusStr = line;
        }

        int status;
        try {
            status = Integer.parseInt(statusStr);
        } 
        catch (NumberFormatException e) {
            throw new ProtocolSyntaxException("Invalid numeric status code");
        }

        if ((status >= 20 && status < 30) || (status >= 40 && status < 60)) {
            for (char c : meta.toCharArray()) {
                if (c < 0x20 || c > 0x7E) {
                    throw new ProtocolSyntaxException("Meta contains invalid character: " + Integer.toHexString(c));
                }
            }
        }
    }
       
    public void format(OutputStream out) throws IOException {
        String header = statusCode + " " + meta + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        if (body != null) {
            out.write(body);
        }
        out.flush();
    }
}
