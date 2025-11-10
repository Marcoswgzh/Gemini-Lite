package gemini_lite;

import java.io.OutputStream;
import java.net.Socket;

import protocol.Request;

public class Client {
     public static void main(String[] args) {
        try {
            Socket socket = new Socket("demo.svc.leastfixedpoint.nl", 1958);
            OutputStream out = socket.getOutputStream();

            Request request = new Request("gemini-lite://demo.svc.leastfixedpoint.nl");

            request.writeTo(out);

            socket.close();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
