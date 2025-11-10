package protocol;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Request {
    private String URI;

    public Request(String URI) {
        this.URI = URI;
    }

    public String getURI() {
        return URI;
    }

    public static Request parse(InputStream in) throws ProtocolSyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b = -1;
        boolean CRFound = false;

        try {
            while ((b = in.read()) != -1) {
                if (CRFound && b == '\n') {
                    break;
                }
                if (b == '\r') {
                    CRFound = true;
                }
                buffer.write(b);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (b == -1) {
            throw new ProtocolSyntaxException("Error: CRLF was not found");
        }
        
        String line = buffer.toString();
        
        if (!line.startsWith("gemini-lite://")) {
            throw new ProtocolSyntaxException("Error: request must start with \"gemini-lite://\"");
        }

        if (line.length() > 1024) {
            throw new ProtocolSyntaxException("Error: request too long");
        }

        return new Request(line);
    }

    public void writeTo(OutputStream out) throws IOException {
        String line = URI + "\r\n";
        out.write(line.getBytes());
    }
}
