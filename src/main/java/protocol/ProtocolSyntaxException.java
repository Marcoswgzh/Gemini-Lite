package protocol;

import java.io.IOException;

public class ProtocolSyntaxException extends IOException{
    public ProtocolSyntaxException(String message) {
        super(message);
    }
}
