import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.*;

/**
 * Multi-threaded TCP Chat Client with File Transfer capabilities.
 * Connects to the server, sends/receives messages, and handles file transfers.
 *
 * Tam Vo
 * CSC 138
 * April 1st, 2025
 * 
 */

public class tcpccs {

    private String serverHostname;
    private int serverPort;
    private String clientUsername;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;

    private Map<String, String> pendingOutgoingFiles = new ConcurrentHashMap<>();

    private static final Path DOWNLOAD_DIR = Paths.get(System.getProperty("user.home"), "Downloads");

    public tcpccs(String hostname, String username) {
        this.serverHostname = hostname;
        this.serverPort = 12345;
        this.clientUsername = username;

        if (!Files.exists(DOWNLOAD_DIR)) {
            try {
                Files.createDirectories(DOWNLOAD_DIR);
            } catch (IOException e) {
                System.err.println("Warning: Could not create download directory: " + DOWNLOAD_DIR);
            }
        }
        if (Files.exists(DOWNLOAD_DIR)) {
            System.out.println("Client Running: Downloads will be directed to " + DOWNLOAD_DIR.toAbsolutePath());
        }
    }

    public void startClient() {
        try {
            socket = new Socket(serverHostname, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(clientUsername);

            System.out.println("Connected to the server. You can start sending messages.");

            Thread listenerThread = new Thread(new MessageListener());
            listenerThread.start();

            handleUserInput();

        } catch (UnknownHostException e) {
            System.err.println("Error: Unknown host " + serverHostname);
            System.exit(1);
        } catch (ConnectException e) {
            System.err.println("Error: Connection refused. Is the server running on " + serverHostname + ":" + serverPort + "?");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.exit(1);
        } finally {
            shutdown();
        }
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        String userInput;
        try {
            while (running) {
                if (scanner.hasNextLine()) {
                    userInput = scanner.nextLine();
                    if (userInput.equalsIgnoreCase("/quit")) {
                        running = false;
                        if (out != null && !out.checkError()) {
                             out.println(userInput);
                        }
                        break;
                    } else if (userInput.startsWith("/sendfile")) {
                        processSendFileCommand(userInput);
                    } else if (userInput.startsWith("/acceptfile")) {
                         out.println(userInput);
                    } else if (userInput.startsWith("/rejectfile")) {
                         out.println(userInput);
                         String[] parts = userInput.trim().split(" ", 2);
                         if (parts.length == 2) {
                             // pendingOutgoingFiles.remove(parts[1]);
                         }
                    } else if (userInput.startsWith("/who")) {
                         out.println(userInput);
                    } else if (userInput.startsWith("/fileport")) {
                         System.out.println("[Client] Error: /fileport command is used internally for file transfers and cannot be sent manually.");
                    } else if (userInput.startsWith("/")) {
                         out.println(userInput);
                    } else {
                        out.println(userInput);
                    }
                } else {
                     if (running) {
                         System.out.println("Console input ended. Sending /quit.");
                         running = false;
                         if (out != null && !out.checkError()) {
                             out.println("/quit");
                         }
                     }
                     break;
                }
            }
        } finally {
            if (running) {
                 shutdown();
            }
        }
    }

    private void processSendFileCommand(String commandLine) {
        String[] parts = commandLine.trim().split(" ", 3);
        if (parts.length != 3) {
            System.out.println("[Client] Usage: /sendfile <recipient_username> <filename>");
            return;
        }
        String recipient = parts[1];
        String filename = parts[2];
        File file = new File(filename);

        if (!file.exists() || !file.isFile()) {
            System.out.println("[Client] Error: File '" + filename + "' not found or is not a regular file.");
            return;
        }
        if (recipient.equals(this.clientUsername)) {
            System.out.println("[Client] Error: Cannot send a file to yourself.");
            return;
        }
        if (pendingOutgoingFiles.containsKey(recipient)) {
             System.out.println("[Client] Error: You already have a pending file offer to " + recipient + ".");
             return;
        }

        long fileSize = file.length();

        pendingOutgoingFiles.put(recipient, filename);
        System.out.println("[Client] Stored pending send request to " + recipient + " for file " + filename);

        String serverCommand = String.format("/sendfile %s %s %d", recipient, filename, fileSize);
        out.println(serverCommand);
    }


    private void startFileSender(String recipientUsername, String filename) {
         if (!filename.equals(pendingOutgoingFiles.getOrDefault(recipientUsername, null))) {
             System.err.println("[Client] Error: No matching pending outgoing file request found for " + recipientUsername + " and file " + filename);
             return;
         }

         pendingOutgoingFiles.remove(recipientUsername);

         File file = new File(filename);
          if (!file.exists() || !file.isFile()) {
              System.out.println("[Client] Error: File '" + filename + "' seems to have disappeared before sending.");
              return;
          }

         ServerSocket fileServerSocket = null;
         try {
             fileServerSocket = new ServerSocket(0);
             int filePort = fileServerSocket.getLocalPort();
             System.out.println("[Client] File transfer server listening on port " + filePort + " for " + filename);

             out.println("/fileport " + filePort + " " + recipientUsername);

             Thread fileSenderThread = new Thread(new FileTransferSender(fileServerSocket, filename));
             fileSenderThread.start();

         } catch (IOException e) {
             System.err.println("[Client] Error starting file transfer server for " + filename + ": " + e.getMessage());
             pendingOutgoingFiles.remove(recipientUsername);
             if (fileServerSocket != null && !fileServerSocket.isClosed()) {
                 try { fileServerSocket.close(); } catch (IOException ce) { /* ignore */ }
             }
         }
    }

     private void startFileReceiver(String senderUsername, String senderIp, int senderPort, String filename) {
         Path safeFileName = Paths.get(filename).getFileName();
         Path filePath = DOWNLOAD_DIR.resolve(safeFileName);

         System.out.println("[Client] Attempting to receive file '" + safeFileName + "' from " + senderUsername + " at " + senderIp + ":" + senderPort);
         System.out.println("[Client] Saving file to: " + filePath.toAbsolutePath());

         try {
              Files.createDirectories(filePath.getParent());
         } catch (IOException e) {
              System.err.println("[Client] Error creating directory for download: " + e.getMessage());
              return;
         }

         Thread fileReceiverThread = new Thread(new FileTransferReceiver(senderIp, senderPort, filePath.toString()));
         fileReceiverThread.start();
     }


    private void shutdown() {
        boolean alreadyShuttingDown = !running;
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) try { in.close(); } catch (IOException e) { /* ignore */ }
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException e) { /* ignore */ }
            }
        } finally {
            if (!alreadyShuttingDown) {
                 System.out.println("\nDisconnected from server.");
            }
        }
    }

    private class MessageListener implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("MessageListener-" + clientUsername);
            try {
                String serverMessage;
                while (running && (serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("[Server]")) {
                         if (serverMessage.contains("wants to send you the file")) {
                              System.out.println(serverMessage);
                         }
                         else if (serverMessage.contains("accepted your file transfer request")) {
                             System.out.println(serverMessage);
                             try {
                                 String recipient = serverMessage.split(" ")[1];
                                 int startQuote = serverMessage.indexOf('\'');
                                 int endQuote = serverMessage.lastIndexOf('\'');
                                 if (startQuote != -1 && endQuote != -1 && startQuote < endQuote) {
                                     String filename = serverMessage.substring(startQuote + 1, endQuote);

                                     if (filename.equals(pendingOutgoingFiles.getOrDefault(recipient, null))) {
                                         System.out.println("[Client] Acceptance confirmed for " + filename + " to " + recipient + ". Starting sender...");
                                         startFileSender(recipient, filename);
                                     } else {
                                         System.err.println("[Client] Received acceptance for an unknown/mismatched file transfer request (Recipient: " + recipient + ", File: " + filename + "). Check pending map.");
                                         System.err.println("[Client] Pending map: " + pendingOutgoingFiles);
                                     }
                                 } else {
                                     throw new Exception("Could not parse filename from acceptance message.");
                                 }
                             } catch (Exception e) {
                                 System.err.println("[Client] Error parsing file acceptance message: " + e.getMessage());
                                 System.err.println("[Client] Received raw acceptance message: " + serverMessage);
                                 System.err.println("[Client] Could not automatically start file sender.");
                             }
                         }
                         else if (serverMessage.contains("is ready for file transfer. Connect to")) {
                              System.out.println(serverMessage);
                              try {
                                   String content = serverMessage.substring(serverMessage.indexOf("]") + 1).trim();
                                   String sender = content.split(" ")[0];
                                   String ip = null;
                                   int port = -1;
                                   String filename = null;

                                   int ipIndex = content.indexOf("Connect to ");
                                   if (ipIndex != -1) {
                                       String afterConnect = content.substring(ipIndex + "Connect to ".length());
                                       ip = afterConnect.split(" ")[0];
                                   }

                                   int portIndex = content.indexOf("on port ");
                                   if (portIndex != -1) {
                                       String afterPort = content.substring(portIndex + "on port ".length());
                                       StringBuilder portNumStr = new StringBuilder();
                                       for (char c : afterPort.toCharArray()) {
                                           if (Character.isDigit(c)) {
                                               portNumStr.append(c);
                                           } else {
                                               break;
                                           }
                                       }
                                       if (portNumStr.length() > 0) {
                                            port = Integer.parseInt(portNumStr.toString());
                                       }
                                   }

                                   int startQuote = serverMessage.indexOf('\'');
                                   int endQuote = serverMessage.lastIndexOf('\'');
                                   if (startQuote != -1 && endQuote != -1 && startQuote < endQuote) {
                                       filename = serverMessage.substring(startQuote + 1, endQuote);
                                   }

                                   if (sender != null && ip != null && port != -1 && filename != null) {
                                       startFileReceiver(sender, ip, port, filename);
                                   } else {
                                       throw new Exception(String.format("Could not parse all required fields (sender=%s, ip=%s, port=%d, file=%s)", sender, ip, port, filename));
                                   }
                              } catch (Exception e) {
                                   System.err.println("[Client] Error parsing file transfer connection info: " + e.getMessage());
                                   System.err.println("[Client] Received raw connection info: " + serverMessage);
                              }
                         } else if (serverMessage.contains("rejected your file transfer request")) {
                              System.out.println(serverMessage);
                              try {
                                   String temp = serverMessage.substring(serverMessage.indexOf("]") + 1).trim();
                                   String recipientWhoRejected = temp.split(" ")[0];
                                   String removedFile = pendingOutgoingFiles.remove(recipientWhoRejected);
                                   if (removedFile != null) {
                                        System.out.println("[Client] Removed pending request to " + recipientWhoRejected + " for file " + removedFile);
                                   } else {
                                        System.out.println("[Client] Received rejection from " + recipientWhoRejected + ", but no matching pending request was found locally.");
                                   }
                              } catch (Exception e) {
                                   System.err.println("[Client] Error removing pending request after rejection: " + e.getMessage());
                              }
                         }
                         else {
                             System.out.println(serverMessage);
                         }
                    } else {
                        System.out.println(serverMessage);
                    }
                }
            } catch (SocketException e) {
                if (running) {
                    System.err.println("\nConnection lost: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("\nError reading from server: " + e.getMessage());
                }
            } finally {
                 if (running) {
                     System.out.println("\nMessage listener stopped. Server disconnected or error occurred.");
                     shutdown();
                 }
            }
        }
    }

    private class FileTransferSender implements Runnable {
        private ServerSocket serverSocket;
        private String filename;

        public FileTransferSender(ServerSocket serverSocket, String filename) {
            this.serverSocket = serverSocket;
            this.filename = filename;
        }

        @Override
        public void run() {
             Thread.currentThread().setName("FileSender-" + filename);
             System.out.println("[Client] File sender thread started for " + filename + ", waiting for receiver connection on port " + serverSocket.getLocalPort() + "...");
             Socket transferSocket = null;
             try {
                 serverSocket.setSoTimeout(30000);
                 transferSocket = serverSocket.accept();
                 transferSocket.setSoTimeout(0);

                 try (FileInputStream fis = new FileInputStream(filename);
                      BufferedInputStream bis = new BufferedInputStream(fis);
                      OutputStream outStream = transferSocket.getOutputStream()) {

                     System.out.println("[Client] Receiver connected for file: " + filename + " from " + transferSocket.getInetAddress().getHostAddress());
                     System.out.println("[Client] Starting file transmission...");

                     byte[] buffer = new byte[8192];
                     int bytesRead;
                     long totalSent = 0;

                     while ((bytesRead = bis.read(buffer)) != -1) {
                         outStream.write(buffer, 0, bytesRead);
                         totalSent += bytesRead;
                     }
                     outStream.flush();
                     try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }

                     System.out.println("\n[Client] File '" + filename + "' sent successfully (" + formatFileSize(totalSent) + ").");

                 }

             } catch (SocketTimeoutException e) {
                 System.err.println("\n[Client] Error: Receiver did not connect within timeout period for file '" + filename + "'. Transfer cancelled.");
             } catch (SocketException e) {
                  System.err.println("\n[Client] Socket Error during file transfer (sending '" + filename + "'): " + e.getMessage() + ". Was the receiver connection closed?");
             } catch (IOException e) {
                 System.err.println("\n[Client] IO Error during file transfer (sending '" + filename + "'): " + e.getMessage());
             } finally {
                 try {
                     if (serverSocket != null && !serverSocket.isClosed()) {
                         serverSocket.close();
                         System.out.println("[Client] Closed file transfer listening socket on port " + serverSocket.getLocalPort());
                     }
                 } catch (IOException e) {
                     System.err.println("[Client] Error closing file transfer server socket: " + e.getMessage());
                 }
                  if (transferSocket != null && !transferSocket.isClosed()) {
                      try { transferSocket.close(); } catch (IOException e) { /* ignore */ }
                  }
                 System.out.println("[Client] File sender thread finished for " + filename);
             }
        }
    }

    private class FileTransferReceiver implements Runnable {
        private String senderIp;
        private int senderPort;
        private String savePath;

        public FileTransferReceiver(String ip, int port, String path) {
            this.senderIp = ip;
            this.senderPort = port;
            this.savePath = path;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("FileReceiver-" + Paths.get(savePath).getFileName());
            System.out.println("[Client] File receiver thread started for " + savePath);
            try (Socket transferSocket = new Socket();
                 FileOutputStream fos = new FileOutputStream(savePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                transferSocket.connect(new InetSocketAddress(senderIp, senderPort), 10000);
                transferSocket.setSoTimeout(60000);

                try (InputStream inStream = transferSocket.getInputStream()) {

                    System.out.println("[Client] Connected to sender " + senderIp + ":" + senderPort + " for file: " + Paths.get(savePath).getFileName());
                    System.out.println("[Client] Starting file reception...");

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalReceived = 0;

                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;
                    }
                    bos.flush();
                    System.out.println("\n[Client] File '" + Paths.get(savePath).getFileName() + "' received successfully (" + formatFileSize(totalReceived) + "). Saved to " + Paths.get(savePath).toAbsolutePath());

                }

            } catch (SocketTimeoutException e) {
                 System.err.println("\n[Client] Timeout during file transfer (receiving to '" + savePath + "'): " + e.getMessage());
                 deletePartialFile();
            } catch (ConnectException e) {
                 System.err.println("\n[Client] Connection refused when trying to receive file from " + senderIp + ":" + senderPort + ". Ensure sender is ready and port is correct.");
                 deletePartialFile();
            } catch (IOException e) {
                System.err.println("\n[Client] IO Error during file transfer (receiving to '" + savePath + "'): " + e.getMessage());
                deletePartialFile();
            } finally {
                 System.out.println("[Client] File receiver thread finished for " + savePath);
            }
        }

        private void deletePartialFile() {
             try {
                 boolean deleted = Files.deleteIfExists(Paths.get(savePath));
                 if (deleted) System.out.println("[Client] Deleted partially received file: " + savePath);
             } catch (IOException ex) {
                  System.err.println("[Client] Error deleting partial file: " + ex.getMessage());
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
        if (args.length != 2) {
            System.out.println("Usage: java tcpccs <server_hostname> <clientUsername>");
            System.exit(1);
        }

        String hostname = args[0];
        String username = args[1];

        if (username.trim().isEmpty() || username.contains(" ") || username.startsWith("/")) {
             System.out.println("Error: Invalid username. Cannot be empty, contain spaces, or start with '/'.");
             System.exit(1);
        }

        tcpccs client = new tcpccs(hostname, username);
        client.startClient();
    }
}
