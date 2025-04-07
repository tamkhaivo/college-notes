import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

/**
 * Multi-threaded TCP Chat Server with File Transfer Coordination.
 * Manages client connections, relays chat messages, and coordinates
 * file transfer requests between clients.
 *
 * Tam Vo
 * CSC 138
 * April 1st, 2025
 * 
 */

public class tcpcss {

    private static final int DEFAULT_PORT = 12345;
    private int port;

    private static ConcurrentHashMap<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, FileTransferRequest> pendingFileTransfers = new ConcurrentHashMap<>();
    private static int clientIdCounter = 0;

    public tcpcss(int port) {
        this.port = port;
    }

    public void startServer() {
        System.out.println("Listener on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for connections...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientIdCounter++;
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }

    private static void broadcast(String message, ClientHandler senderHandler) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers.values()) {
                if (handler != senderHandler) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    private static boolean sendDirectMessage(String message, String recipientUsername) {
         ClientHandler recipientHandler = clientHandlers.get(recipientUsername);
         if (recipientHandler != null) {
             recipientHandler.sendMessage(message);
             return true;
         } else {
             return false;
         }
    }

    private static void removeClient(String username, ClientHandler handler) {
        if (username != null) {
            ClientHandler removedHandler;
            synchronized (clientHandlers) {
                removedHandler = clientHandlers.remove(username);
            }
            if (removedHandler != null) {
                System.out.println("[" + username + "] has left the chat.");
                broadcast("[" + username + "] has left the chat.", handler);
                cleanupPendingTransfers(username);
            }
        }
    }

    private static void handleFileSendRequest(ClientHandler senderHandler, String recipientUsername, String filename, String fileSizeStr) {
        String senderUsername = senderHandler.getUsername();
        long fileSize;
        try {
            fileSize = Long.parseLong(fileSizeStr);
            if (fileSize < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            senderHandler.sendMessage("[Server] Invalid file size format.");
            return;
        }

        if (senderUsername.equals(recipientUsername)) {
            senderHandler.sendMessage("[Server] You cannot send a file to yourself.");
            return;
        }

        ClientHandler recipientHandler = clientHandlers.get(recipientUsername);
        if (recipientHandler == null) {
            senderHandler.sendMessage("[Server] User '" + recipientUsername + "' not found or is not online.");
            return;
        }

        String requestKey = recipientUsername + "_" + senderUsername;

        FileTransferRequest request = new FileTransferRequest(senderUsername, recipientUsername, filename, fileSize);
        FileTransferRequest existing = pendingFileTransfers.putIfAbsent(requestKey, request);

        if (existing != null) {
             senderHandler.sendMessage("[Server] You already have a pending file transfer request to " + recipientUsername + ".");
             return;
        }

        String notificationMsg = String.format("[File transfer initiated from %s to %s %s (%s)]",
                                         senderUsername, recipientUsername, filename, formatFileSize(fileSize));
        broadcast(notificationMsg, null);

        String recipientMsg = String.format("[Server] %s wants to send you the file '%s' (%s). Use /acceptfile %s or /rejectfile %s.",
                                            senderUsername, filename, formatFileSize(fileSize), senderUsername, senderUsername);
        sendDirectMessage(recipientMsg, recipientUsername);
    }

    private static void handleFileAccept(ClientHandler recipientHandler, String senderUsername) {
        String recipientUsername = recipientHandler.getUsername();
        String requestKey = recipientUsername + "_" + senderUsername;

        FileTransferRequest request = pendingFileTransfers.get(requestKey);
        if (request == null) {
            recipientHandler.sendMessage("[Server] No pending file transfer request found from user '" + senderUsername + "'.");
            return;
        }

        ClientHandler senderHandler = clientHandlers.get(senderUsername);
        if (senderHandler == null) {
             recipientHandler.sendMessage("[Server] Sender '" + senderUsername + "' is no longer online. Cannot accept file.");
             pendingFileTransfers.remove(requestKey);
             return;
        }

        String acceptMsg = String.format("[File transfer accepted from %s to %s]", recipientUsername, senderUsername);
        broadcast(acceptMsg, null);

        String senderInstruction = String.format(
            "[Server] %s accepted your file transfer request for '%s'. Please start the file transfer server and send port number using /fileport <port> %s.",
            recipientUsername, request.getFilename(), recipientUsername);
        sendDirectMessage(senderInstruction, senderUsername);

    }

    private static void handleFilePort(ClientHandler senderHandler, String portStr, String recipientUsername) {
        String senderUsername = senderHandler.getUsername();
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new NumberFormatException("Port out of range");
        } catch (NumberFormatException e) {
            senderHandler.sendMessage("[Server] Invalid port number: " + portStr);
            return;
        }

        String requestKey = recipientUsername + "_" + senderUsername;
        FileTransferRequest request = pendingFileTransfers.get(requestKey);

        if (request == null) {
            senderHandler.sendMessage("[Server] Could not find matching accepted file transfer request for recipient '" + recipientUsername + "'.");
            return;
        }

        if (!clientHandlers.containsKey(recipientUsername)) {
            senderHandler.sendMessage("[Server] Recipient '" + recipientUsername + "' disconnected before transfer could start.");
            pendingFileTransfers.remove(requestKey);
            return;
        }

        String senderIp = senderHandler.getClientAddress();
        if (senderIp == null) {
            senderHandler.sendMessage("[Server] Could not determine your IP address for file transfer.");
             return;
        }

        String filename = request.getFilename();

        String recipientMsg = String.format("[Server] %s is ready for file transfer. Connect to %s on port %d to receive '%s'.",
                                            senderUsername, senderIp, port, filename);
        boolean notified = sendDirectMessage(recipientMsg, recipientUsername);

        if (notified) {
            String startMsg = String.format("[Starting file transfer between %s and %s]", senderUsername, recipientUsername);
            broadcast(startMsg, null);
            pendingFileTransfers.remove(requestKey);
        } else {
            senderHandler.sendMessage("[Server] Failed to notify recipient '" + recipientUsername + "'. They may have disconnected.");
             pendingFileTransfers.remove(requestKey);
        }
    }

    private static void handleFileReject(ClientHandler recipientHandler, String senderUsername) {
        String recipientUsername = recipientHandler.getUsername();
        String requestKey = recipientUsername + "_" + senderUsername;

        FileTransferRequest request = pendingFileTransfers.remove(requestKey);

        if (request == null) {
            recipientHandler.sendMessage("[Server] No pending file transfer request found from user '" + senderUsername + "'.");
            return;
        }

        sendDirectMessage("[Server] " + recipientUsername + " rejected your file transfer request for '" + request.getFilename() + "'.", senderUsername);
        recipientHandler.sendMessage("[Server] You rejected the file transfer from " + senderUsername + ".");

        System.out.println("[" + recipientUsername + " rejected file transfer from " + senderUsername + "]");
    }

     private static void handleWhoRequest(ClientHandler requesterHandler) {
         StringBuilder userListBuilder = new StringBuilder("[Online users: ");
         List<String> usernames = new ArrayList<>();
         synchronized (clientHandlers) {
             usernames.addAll(clientHandlers.keySet());
         }
         Collections.sort(usernames);

         boolean first = true;
         for (String username : usernames) {
             if (!first) {
                 userListBuilder.append(", ");
             }
             userListBuilder.append(username);
             first = false;
         }
         userListBuilder.append("]");
         String userList = userListBuilder.toString();

         System.out.println("[" + requesterHandler.getUsername() + "] requested online users list.");

         broadcast(userList, null);
     }

    private static void cleanupPendingTransfers(String disconnectedUsername) {
        synchronized (pendingFileTransfers) {
            Iterator<Map.Entry<String, FileTransferRequest>> iterator = pendingFileTransfers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, FileTransferRequest> entry = iterator.next();
                FileTransferRequest request = entry.getValue();
                String otherParty = null;

                if (request.getSenderUsername().equals(disconnectedUsername)) {
                    otherParty = request.getRecipientUsername();
                } else if (request.getRecipientUsername().equals(disconnectedUsername)) {
                    otherParty = request.getSenderUsername();
                }

                if (otherParty != null) {
                    sendDirectMessage("[Server] File transfer with " + disconnectedUsername + " cancelled because they disconnected.", otherParty);
                    iterator.remove();
                    System.out.println("Removed pending file transfer involving disconnected user: " + disconnectedUsername + " and " + otherParty);
                }
            }
        }
    }

    private static String formatFileSize(long size) {
        if (size < 0) return "Invalid size";
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            double kb = size / 1024.0;
            return (kb == Math.floor(kb)) ? String.format("%d KB", (long)kb) : String.format("%.1f KB", kb);
        } else {
             double mb = size / (1024.0 * 1024.0);
            return String.format("%.1f MB", mb);
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port <= 0 || port > 65535) {
                    System.err.println("Invalid port number: " + args[0] + ". Must be between 1 and 65535.");
                    System.err.println("Using default port " + DEFAULT_PORT);
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number format: " + args[0] + ". Using default port " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        tcpcss server = new tcpcss(port);
        server.startServer();
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username = null;
        private int clientId;
        private String clientIp;
        private int clientPort;

        public ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
            try {
                 this.clientIp = socket.getInetAddress().getHostAddress();
                 this.clientPort = socket.getPort();
                 System.out.printf("New connection, thread name is %s, ip is: %s, port: %d\n",
                                   Thread.currentThread().getName(), clientIp, clientPort);
                 System.out.println("Adding to list of sockets as " + id);
            } catch (Exception e) {
                 System.err.println("Error getting client IP/Port for ID " + id + ": " + e.getMessage());
                 this.clientIp = "UNKNOWN";
                 this.clientPort = -1;
            }
        }

        public String getUsername() {
            return username;
        }

        public String getClientAddress() {
            return clientIp;
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
                if (out.checkError()) {
                     System.err.println("Error sending message to " + (username != null ? username : "client " + clientId));
                }
            } else {
                 System.err.println("Cannot send message, PrintWriter is null for " + (username != null ? username : "client " + clientId));
            }
        }

        @Override
        public void run() {
            Thread.currentThread().setName("ClientHandler-" + clientId + "-Connecting");

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String receivedUsername = in.readLine();

                if (receivedUsername == null || receivedUsername.trim().isEmpty() || receivedUsername.contains(" ") || receivedUsername.startsWith("/")) {
                    System.err.printf("Client %d (%s:%d) sent invalid username ('%s'). Disconnecting.\n",
                                      clientId, clientIp, clientPort, receivedUsername);
                    sendMessage("[Server] Error: Invalid username. Cannot be empty, contain spaces, or start with '/'. Disconnecting.");
                    return;
                }

                Thread.currentThread().setName("ClientHandler-" + clientId + "-" + receivedUsername);
                System.out.printf("Thread %s started for client %d (%s:%d) with username %s\n",
                                  Thread.currentThread().getName(), clientId, clientIp, clientPort, receivedUsername);

                ClientHandler existingHandler = clientHandlers.putIfAbsent(receivedUsername, this);
                if (existingHandler != null) {
                    System.err.println("Username '" + receivedUsername + "' already taken. Disconnecting client " + clientId);
                    sendMessage("[Server] Error: Username '" + receivedUsername + "' is already taken. Disconnecting.");
                    clientHandlers.remove(receivedUsername, this);
                    return;
                }

                this.username = receivedUsername;

                System.out.println("[" + this.username + "] has joined the chat.");
                broadcast("[" + this.username + "] has joined the chat.", this);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (this.username == null) break;

                    if (inputLine.startsWith("/")) {
                        processCommand(inputLine);
                    } else {
                        String message = "[" + this.username + "] " + inputLine;
                        System.out.println(message);
                        broadcast(message, this);
                    }
                }
            } catch (SocketException e) {
                if (!clientSocket.isClosed()) {
                     System.out.println("SocketException for user " + (username != null ? username : clientId) + ": " + e.getMessage() + ". Client likely disconnected abruptly.");
                }
            } catch (IOException e) {
                 if (!clientSocket.isClosed()) {
                    System.err.println("IOException for user " + (username != null ? username : clientId) + ": " + e.getMessage());
                 }
            } catch (Exception e) {
                 System.err.println("Unexpected error in ClientHandler for " + (username != null ? username : clientId) + ": " + e.getMessage());
                 e.printStackTrace();
            }
            finally {
                removeClient(this.username, this);
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing resources for " + (username != null ? username : clientId) + ": " + e.getMessage());
                }
                 System.out.printf("Thread %s finished for client %d (%s)\n",
                                  Thread.currentThread().getName(), clientId, (username != null ? username : "UNKNOWN"));
            }
        }

        private void processCommand(String commandLine) {
            String[] parts = commandLine.trim().split(" ", 4);
            String command = parts[0].toLowerCase();

             if (this.username == null) {
                 System.err.println("Received command '" + command + "' before username was set for client " + clientId);
                 sendMessage("[Server] Error: Cannot process commands before username is set.");
                 return;
             }

            switch (command) {
                case "/quit":
                    try { clientSocket.close(); } catch (IOException e) {}
                    break;
                case "/who":
                    if (parts.length == 1) {
                        handleWhoRequest(this);
                    } else {
                        sendMessage("[Server] Usage: /who");
                    }
                    break;
                case "/sendfile":
                    if (parts.length == 4) {
                         handleFileSendRequest(this, parts[1], parts[2], parts[3]);
                    } else {
                        sendMessage("[Server] Internal Error: Malformed /sendfile received.");
                        System.err.println("Malformed /sendfile received from " + username + ": " + commandLine);
                    }
                    break;
                case "/acceptfile":
                    if (parts.length == 2) {
                        handleFileAccept(this, parts[1]);
                    } else {
                        sendMessage("[Server] Usage: /acceptfile <sender>");
                    }
                    break;
                case "/rejectfile":
                    if (parts.length == 2) {
                        handleFileReject(this, parts[1]);
                    } else {
                        sendMessage("[Server] Usage: /rejectfile <sender>");
                    }
                    break;
                 case "/fileport":
                    parts = commandLine.trim().split(" ", 3);
                    if (parts.length == 3) {
                        handleFilePort(this, parts[1], parts[2]);
                    } else {
                        sendMessage("[Server] Usage: /fileport <port> <recipientUsername>");
                        System.err.println("Malformed /fileport received from " + username + ": " + commandLine);
                    }
                    break;
                default:
                    sendMessage("[Server] Unknown command: " + command);
                    break;
            }
        }
    }

     private static class FileTransferRequest {
         private final String senderUsername;
         private final String recipientUsername;
         private final String filename;
         private final long fileSize;
         private final long timestamp;

         public FileTransferRequest(String sender, String recipient, String file, long size) {
             this.senderUsername = sender;
             this.recipientUsername = recipient;
             this.filename = file;
             this.fileSize = size;
             this.timestamp = System.currentTimeMillis();
         }

         public String getSenderUsername() { return senderUsername; }
         public String getRecipientUsername() { return recipientUsername; }
         public String getFilename() { return filename; }
         public long getFileSize() { return fileSize; }
         public long getTimestamp() { return timestamp; }

     }
}
