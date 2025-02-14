import java.net.*;
import java.io.IOException;

public class TimedPingClient {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SimpleTimedPingClient <host>"); // Bad Script Usage
            return;
        }
        String host = args[0];
        DatagramSocket socket = null;
        int port = 7;
        int packetSize = 1024;
        try {
            InetAddress address = InetAddress.getByName(host);
            socket = new DatagramSocket(32);
            socket.setSoTimeout(5000); // Timeout after 5s
            socket.setReceiveBufferSize(packetSize); // RX Buffer Size 1024
            byte[] message = "ping".getBytes();
            InetSocketAddress dest = new InetSocketAddress(address, port);
            DatagramPacket outgoingDatagram = new DatagramPacket(message, message.length, dest);

            long startTime = System.currentTimeMillis();
            socket.send(outgoingDatagram);
            socket.receive(new DatagramPacket(new byte[packetSize], packetSize));
            long endTime = System.currentTimeMillis();

            long rtt = endTime - startTime; // Calculate RTT in ms

            System.out.println(host + " is reachable. RTT: " + rtt + " ms");

        } catch (UnknownHostException e) { // Host not found
            System.out.println(host + " is not reachabl1e.");
        } catch (IOException e) { // Timeout
            System.out.println(host + " is not reachable.");
        } finally {
            if (socket != null && !socket.isClosed()) { // Close Socket
                socket.close();
            }
        }
    }
}