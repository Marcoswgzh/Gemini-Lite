package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Request {
    private final String url;
    private final String input;

    public Request(String url, String input) {
        this.url = url;
        this.input = input;
    }

    public String getUrl() {
        return url;
    }

    public String getInput() {
        return input;
    }

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

    public static void checkSyntax(String line) throws ProtocolSyntaxException {
        if (line.length() > 1024) {
            throw new ProtocolSyntaxException("Request line too long (>1024)");
        }
        
        if (!line.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Request does not begin with \"gemini-lite://\"");
        }
    }

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
