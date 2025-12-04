package protocol;

import java.io.IOException;

/**
 * Thrown to indicate a syntax violation in the Gemini-Lite protocol
 * It extends IOException since syntax errors occur during stream-based I/O operations.
 */
public class ProtocolSyntaxException extends IOException{
    /**
     * Constructs a new ProtocolSyntaxException with the specified message.
     * @param message: a descriptive error message explaining the syntax violation
     */
    public ProtocolSyntaxException(String message) {
        super(message);
    }
}