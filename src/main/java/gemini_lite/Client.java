package gemini_lite;

import java.io.IOException;

import engines.ClientEngine;
import engines.ProxyEngine;
import protocol.ProtocolSyntaxException;

/**
 * Gemini-lite client application
 * This class is responsible for:
 * Parsing command-line arguments
 * Initializing optional proxy configuration
 * Launching the ClientEngine
 */
public class Client {
    /**
     * Program entry point
     *
     * @param args: comman-line arguments:
     * args[0]: Target URL (required)
     * args[1]: Input parameter (optional)
     */
    public static void main(String[] args) {
        System.exit(runClientMain(args));
    }  

    /**
     * Executes the main client logic and returns a process exit code.
     * This method:
     * Validates command-line arguments
     * Initializes proxy configuration from environment variables
     * Invokes the ClientEngine
     * @param args: command-line arguments
     * @return 0 on success or 1 on failure
     */
    private static int runClientMain(String[] args) {
        try {
            validateArgs(args);

            initProxy();

            String url = args[0];
            String input = null;
            if (args.length == 2) {
                input = args[1];
            }

            ClientEngine.runEngine(url, null, input);
            return 0; 
        } 
        catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
            return 1;

        } 
        catch (ProtocolSyntaxException e) {
            System.err.println("Protocol syntax error: " + e.getMessage());
            return 1;

        } 
        catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Validates the command-line arguments
     * The client only accepts 1 or 2 arguments (URL or URL and input)
     * @param args: Command-line arguments to validate
     * @throws IllegalArgumentException if the argument count is invalid
     */
    private static void validateArgs(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -cp <jar> gemini_lite.Client <URL> [<input>]");
        }
    }

    /**
     * Initializes proxy configuration from the GEMINI_LITE_PROXY environment variable
     * If the variable is present and non-empty, proxy routing is enabled
     * and parsed via ProxyEngine.parseProxy(String)
     */
    private static void initProxy() {
        String proxyEnv = System.getenv("GEMINI_LITE_PROXY");
        if (proxyEnv != null && !proxyEnv.isEmpty()) {
            ProxyEngine.parseProxy(proxyEnv);
            ProxyEngine.useProxy = true;
        }
    }
}
