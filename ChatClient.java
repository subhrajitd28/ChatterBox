import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private static final int MAX_MESSAGE_SIZE = 256;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter Server IP: ");
            String serverIP = scanner.nextLine().trim();
            System.out.print("Enter Server Port: ");
            int port = scanner.nextInt();
            scanner.nextLine(); // consume newline

            // Socket socket = new Socket(serverIP, port);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(serverIP, port), 3000);
            System.out.println("Connected to the chatroom.");

            // ** was here
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println(in.readLine()); // prompt for username
            String username = scanner.nextLine();
            out.println(username);
            
            new Thread(new MessageReceiver(socket)).start(); // **

            while (true) {
                String message = scanner.nextLine();
                if (message.length() > MAX_MESSAGE_SIZE) {
                    System.out.println("Message too long! Limit is " + MAX_MESSAGE_SIZE + " characters.");
                    continue;
                }
                out.println(message);
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MessageReceiver implements Runnable {
        private Socket socket;

        public MessageReceiver(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }
    }
}
