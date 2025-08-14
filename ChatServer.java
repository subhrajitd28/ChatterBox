import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int MAX_USERS = 5;
    private static final int MAX_MESSAGE_SIZE = 256;
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter Server IP (or 0.0.0.0 for all interfaces): ");
            String host = scanner.nextLine().trim();
            System.out.print("Enter Server Port: ");
            int port = scanner.nextInt();

            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port));
            System.out.println("Server started on " + host + ":" + port);

            while (true) {
                Socket socket = serverSocket.accept();

                if (clientHandlers.size() >= MAX_USERS) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("Chatroom full. Try again later.");
                    socket.close();
                    continue;
                }

                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Enter your username: ");
                username = in.readLine().trim();

                System.out.println(username + " joined the chat.");
                broadcast(username + " has joined the chat.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.length() > MAX_MESSAGE_SIZE) {
                        out.println("Message too long! Limit is " + MAX_MESSAGE_SIZE + " characters.");
                        continue;
                    }
                    if (message.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    System.out.println(username + ": " + message);
                    broadcast(username + ": " + message, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println(username + " left the chat.");
                broadcast(username + " has left the chat.", this);
                removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
