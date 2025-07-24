package master;

import java.io.*;
import java.net.Socket;
public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientId;
    private PrintWriter out;
    private BufferedReader reader; // Add this

    public ClientHandler(Socket socket, String clientId, BufferedReader reader) {
        this.socket = socket;
        this.clientId = clientId;
        this.reader = reader; // Assign passed reader
    }
    public void sendCommand(String command) {
        System.out.println("[DEBUG] Preparing to send command to " + clientId + ": " + command);
        if (out != null) {
            out.println(command);
            out.flush();
            System.out.println("[INFO] Command sent to " + clientId + ": " + command);
        } else {
            System.err.println("[WARN] Output stream is null for " + clientId);
        }
    }



    @Override
    public void run() {
        System.out.println("[DEBUG] ClientHandler started for " + clientId);
        try (OutputStream output = socket.getOutputStream()) {
            out = new PrintWriter(output, true);
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FROM CLIENT " + clientId + "] " + line);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Connection lost with " + clientId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("[INFO] Socket closed for " + clientId);
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to close socket for " + clientId + ": " + e.getMessage());
            }
        }
    }
}
