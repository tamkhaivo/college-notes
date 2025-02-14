import java.net.*;
import java.io.IOException;

public class SimplePingClient {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SimplePingClient <host>"); // Bad Script Usage
            return;
        }

        String host = args[0];
        try {
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(5000); // 5s timeout
            if (reachable) {
                System.out.println(host + " is reachable.");
            }
        } catch (UnknownHostException e) {
            System.out.println(host + " is not reachable."); // InetAddress.getByName(host) Error Handler
        } catch (IOException e) {
            System.out.println(host + " is not reachable."); // address.isReachable(timeout) Error Handler
        }
    }
}