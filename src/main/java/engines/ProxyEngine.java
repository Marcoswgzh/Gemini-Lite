package engines;

public class ProxyEngine {
    public static String proxyHost;
    public static int proxyPort;
    public static boolean useProxy = false;

    public static void parseProxy(String proxyEnv) {
        int colonIdx = proxyEnv.indexOf(':');
        if (colonIdx == -1) {
            System.err.println("Invalid GEMINI_LITE_PROXY format. Expected hostname:port");
            System.exit(1);
        }
        proxyHost = proxyEnv.substring(0, colonIdx);
        try {
            proxyPort = Integer.parseInt(proxyEnv.substring(colonIdx + 1));
        } 
        catch (NumberFormatException e) {
            System.err.println("Invalid GEMINI_LITE_PROXY port: " + e.getMessage());
            System.exit(1);
        }
    }

    

}
