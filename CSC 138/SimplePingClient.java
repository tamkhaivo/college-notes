import java.net.*;

public class UDPSocketExample {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        System.out.println("Args"+ address);
        InetAddress address = InetAddress.getByName("google.com");
        byte[] data = "test".getBytes();
        
        DatagramPacket packet = new DatagramPacket(data, data.length, address, 7);
        socket.send(packet);
        boolean reachable = address.isReachable(5000); // 5-second timeout

        System.out.println("Packet sent to " + address);
        
        socket.close();
    }
}
