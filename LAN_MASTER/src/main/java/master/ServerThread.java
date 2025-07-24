package master;

import javax.swing.DefaultListModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class ServerThread implements Runnable {
    private int port;
    private Map<String, ClientHandler> clients;
    private DefaultListModel<String> deviceListModel;

    public ServerThread(int port, Map<String, ClientHandler> clients, DefaultListModel<String> deviceListModel) {
        this.port = port;
        this.clients = clients;
        this.deviceListModel = deviceListModel;
        this.run();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[DEBUG] Server started on port " + port);
            while (true) {
                System.out.println("[DEBUG] Waiting for client connection...");
                Socket clientSocket = serverSocket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientId = in.readLine(); // Read host | ip | mac sent by client
                System.out.println("[INFO] Client identified as: " + clientId);

                deviceListModel.addElement(clientId);

                ClientHandler handler = new ClientHandler(clientSocket, clientId, in);
                clients.put(clientId, handler);
                new Thread(handler).start();

                System.out.println("[DEBUG] ClientHandler thread started for " + clientId);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
