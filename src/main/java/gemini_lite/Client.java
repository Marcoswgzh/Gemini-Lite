package gemini_lite;

import java.io.IOException;

import engines.ClientEngine;
import engines.ProxyEngine;
import protocol.ProtocolSyntaxException;

public class Client {
    public static void main(String[] args) {
        System.exit(runClientMain(args));
    }

    private static int runClientMain(String[] args) {
        try {
            validateArgs(args);

            initProxy();

            String url = args[0];
            String input = null;
            if (args.length == 2) {
                input = args[1];
            }

            ClientEngine.runEngine(url, input);
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
    
    private static void validateArgs(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -cp <jar> gemini_lite.Client <URL> [<input>]");
        }
    }

    private static void initProxy() {
        String proxyEnv = System.getenv("GEMINI_LITE_PROXY");
        if (proxyEnv != null && !proxyEnv.isEmpty()) {
            ProxyEngine.parseProxy(proxyEnv);
            ProxyEngine.useProxy = true;
        }
    }
}
