package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Reply {
    public int statusCode;
    public String meta;
    public byte[] body;

    public Reply(int statusCode, String meta, byte[] body) {
        this.statusCode = statusCode;
        this.meta = meta;
        this.body = body;
    }

    public static Reply parse(InputStream in) throws ProtocolSyntaxException, IOException {
        String header = readLineCRLF(in);
        int spaceIndex = header.indexOf(' ');
        if (spaceIndex < 0 || spaceIndex > 3) {
            throw new ProtocolSyntaxException("Error: reply header must have status space meta");
        } 
        int code;
        try {
            code = Integer.parseInt(header.substring(0, spaceIndex));
        } 
        catch (NumberFormatException e) {
            throw new ProtocolSyntaxException("Error: reply status code not an integer");
        }
        String meta = header.substring(spaceIndex + 1);

        byte[] body = null;
        if (code / 10 == 2) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
               bout.write(buf, 0, n); 
            } 
            body = bout.toByteArray();
        }
        return new Reply(code, meta, body);
    }

    private static String readLineCRLF(InputStream in) throws IOException, ProtocolSyntaxException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        boolean CRFound = false;
        while ((b = in.read()) != -1) {
            if (CRFound && b == '\n') {
                break;
            }
            if (b == '\r') {
                CRFound = true;
            }
            if (!CRFound && b != '\n') {
                buf.write(b);
            } 
        }

        if (!CRFound) {
            throw new ProtocolSyntaxException("Error: CRLF was not found");
        }

        return buf.toString(StandardCharsets.UTF_8);
    }
       
    public void format(OutputStream out) throws IOException {
        String header = String.format("%d %s\r\n", statusCode, meta);
        out.write(header.getBytes(StandardCharsets.UTF_8));
        if (body != null) {
            out.write(body);
        }
        out.flush();
    }
}
