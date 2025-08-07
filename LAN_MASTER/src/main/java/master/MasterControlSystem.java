package master;// ==========================================
// Full Java Master-Client Control System (Updated)
// With Screen Capture Receive and Display
// ==========================================

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class MasterControlSystem extends JFrame {
    private JTextArea logArea;
    private JButton btnStartServer, btnShutdown, btnRestart, btnSendFile, btnBroadcast, btnCaptureWebcam, btnCaptureScreen;
    private DefaultListModel<String> deviceListModel;
    private JList<String> deviceList;
    private Map<String, Socket> deviceSockets = new HashMap<>();
    private JLabel imageLabel;

    public MasterControlSystem() {
        setTitle("Master Control System");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        deviceListModel = new DefaultListModel<>();
        deviceList = new JList<>(deviceListModel);
        logArea = new JTextArea();
        logArea.setEditable(false);
        imageLabel = new JLabel();

        btnStartServer = new JButton("Start Server");
        btnShutdown = new JButton("Shutdown");
        btnRestart = new JButton("Restart");
        btnSendFile = new JButton("Send File");
        btnBroadcast = new JButton("Broadcast Message");
        btnCaptureWebcam = new JButton("Capture Webcam");
        btnCaptureScreen = new JButton("Capture Screen");

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Connected Devices"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(deviceList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(7, 1));
        buttonPanel.add(btnStartServer);
        buttonPanel.add(btnShutdown);
        buttonPanel.add(btnRestart);
        buttonPanel.add(btnSendFile);
        buttonPanel.add(btnBroadcast);
        buttonPanel.add(btnCaptureWebcam);
        buttonPanel.add(btnCaptureScreen);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(imageLabel, BorderLayout.EAST);

        btnStartServer.addActionListener(e -> startServer());
        btnShutdown.addActionListener(e -> sendCommand("SHUTDOWN"));
        btnRestart.addActionListener(e -> sendCommand("RESTART"));
        btnBroadcast.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog("Enter message:");
            sendCommand("BROADCAST:" + msg);
        });
        btnCaptureWebcam.addActionListener(e -> sendCommand("CAPTURE_WEBCAM"));
        btnCaptureScreen.addActionListener(e -> sendCommand("CAPTURE_SCREEN"));

        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9090)) {
                log("Server started on port 9090");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String clientInfo = in.readLine();
                    log("Connected: " + clientInfo);
                    deviceListModel.addElement(clientInfo);
                    deviceSockets.put(clientInfo, clientSocket);
                    listenToClient(clientInfo, clientSocket);

                }
            } catch (IOException ex) {
                log("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void sendCommand(String cmd) {
        String target = deviceList.getSelectedValue();
        if (target == null) return;
        try {
            Socket s = deviceSockets.get(target);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            out.println(cmd);
            log("Sent to " + target + ": " + cmd);
        } catch (IOException e) {
            log("Error sending command: " + e.getMessage());
        }
    }

    private void listenToClient(String client, Socket socket) {
        new Thread(() -> {
            try {
                InputStream in = socket.getInputStream();
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                while (true) {
                    int imageLength = dis.readInt(); // Read image length
                    byte[] imageData = new byte[imageLength];
                    dis.readFully(imageData); // Read full image
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(data)) != -1) {
                        buffer.write(data, 0, bytesRead);
                        if (bytesRead < 4096) break; // Assuming end of image
                    }

                    imageData = buffer.toByteArray();
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                    if (image != null) {
                        ImageIcon icon = new ImageIcon(image.getScaledInstance(400, 300, Image.SCALE_SMOOTH));
                        SwingUtilities.invokeLater(() -> imageLabel.setIcon(icon));
                        log("Image received and displayed from: " + client);
                    }
                }
            } catch (IOException e) {
                log("Connection closed with: " + client);
            }
        }).start();
    }


    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MasterControlSystem::new);
    }
}
