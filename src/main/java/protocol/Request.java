package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Request {
    private String url;
    private String input;

    public Request(String url, String input) {
        this.url = url;
        this.input = input;
    }

    public String geturl() {
        return url;
    }

    public static Request parse(InputStream in) throws ProtocolSyntaxException, IOException {
        String line = readLineCRLF(in);
        checkSyntax(line);

        String a = null;
        String input = null;

        int spaceIndex = line.indexOf(" ");
        if (spaceIndex >= 0) {
            a = line.substring(0, spaceIndex);
            input = line.substring(spaceIndex + 1);
        } else {
            a = line;
        }
        return new Request(a, input);
    } 
    
    private static String readLineCRLF(InputStream in) throws IOException, ProtocolSyntaxException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b = -1;
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

    public static void checkSyntax(String line) throws ProtocolSyntaxException {
        if (!line.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Error: Request does not begin with \"gemini-lite://\"");
        }
        if (line.length() > 1024) {
            throw new ProtocolSyntaxException("Error: request too long");
        }
    }

    public void format(OutputStream out) throws IOException {
        String line = url;
        if (input != null) line += " " + input;
        line += "\r\n";
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
