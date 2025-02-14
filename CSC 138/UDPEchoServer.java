
// UDPEchoServer.java
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPEchoServer {
    public static void main(String[] args) {
        int port = 7; // Port to listen on

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP Echo Server is running on port " + port + "...");

            byte[] buffer = new byte[1024];

            while (true) {
                // Receive packet
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);

                // Get sender's address and port
                InetAddress clientAddress = requestPacket.getAddress();
                int clientPort = requestPacket.getPort();

                // Send the same data back
                DatagramPacket responsePacket = new DatagramPacket(
                        requestPacket.getData(),
                        requestPacket.getLength(),
                        clientAddress,
                        clientPort);

                socket.send(responsePacket);
                System.out.println("Echoed packet back to " + clientAddress + ":" + clientPort);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
