package master;

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
    private JButton btnStartServer, btnShutdown, btnRestart, btnSendFile, btnBroadcast, btnStartStream, btnStopStream;
    private DefaultListModel<String> deviceListModel;
    private JList<String> deviceList;
    private Map<String, Socket> deviceSockets = new HashMap<>();
    private JLabel imageLabel;
    private boolean streaming = false;

    public MasterControlSystem() {
        setTitle("Master Control System - Live Streaming");
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
        btnStartStream = new JButton("Start Screen Stream");
        btnStopStream = new JButton("Stop Screen Stream");

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Connected Devices"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(deviceList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(8, 1));
        buttonPanel.add(btnStartServer);
        buttonPanel.add(btnShutdown);
        buttonPanel.add(btnRestart);
        buttonPanel.add(btnSendFile);
        buttonPanel.add(btnBroadcast);
        buttonPanel.add(btnStartStream);
        buttonPanel.add(btnStopStream);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.EAST);
        add(imageLabel, BorderLayout.CENTER);

        btnStartServer.addActionListener(e -> {
            startServer();
            startImageServer();
        });
        btnShutdown.addActionListener(e -> sendCommand("SHUTDOWN"));
        btnRestart.addActionListener(e -> sendCommand("RESTART"));
        btnBroadcast.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog("Enter message:");
            sendCommand("BROADCAST:" + msg);
        });
        btnStartStream.addActionListener(e -> startScreenStream());
        btnStopStream.addActionListener(e -> stopScreenStream());

        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9090)) {
                log("Command server started on port 9090");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String clientInfo = in.readLine();
                    log("Connected: " + clientInfo);
                    deviceListModel.addElement(clientInfo);
                    deviceSockets.put(clientInfo, clientSocket);
                }
            } catch (IOException ex) {
                log("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void startImageServer() {
        new Thread(() -> {
            try (ServerSocket imageServerSocket = new ServerSocket(9091)) {
                log("Image server started on port 9091");
                while (true) {
                    Socket imageSocket = imageServerSocket.accept();
                    new Thread(() -> handleImageSocket(imageSocket)).start();
                }
            } catch (IOException e) {
                log("Image server error: " + e.getMessage());
            }
        }).start();
    }

    private void handleImageSocket(Socket imageSocket) {
        try {
            DataInputStream dis = new DataInputStream(imageSocket.getInputStream());
            while (streaming) { // keep receiving frames as long as streaming=true
                int imageLength = dis.readInt();
                byte[] imageData = new byte[imageLength];
                dis.readFully(imageData);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                if (image != null) {
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(800, 600, Image.SCALE_SMOOTH));
                    SwingUtilities.invokeLater(() -> imageLabel.setIcon(icon));
                }
            }
            dis.close();
            imageSocket.close();
        } catch (IOException e) {
            log("Streaming stopped: " + e.getMessage());
        }
    }

    private void sendCommand(String cmd) {
        String target = deviceList.getSelectedValue();
        if (target == null) {
            log("No client selected.");
            return;
        }

        try {
            Socket s = deviceSockets.get(target);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            out.println(cmd);
            log("Sent to " + target + ": " + cmd);
        } catch (IOException e) {
            log("Error sending command: " + e.getMessage());
        }
    }

    private void startScreenStream() {
        streaming = true;
        sendCommand("START_STREAM");
        log("Requested client to start screen streaming...");
    }

    private void stopScreenStream() {
        streaming = false;
        sendCommand("STOP_STREAM");
        log("Requested client to stop screen streaming...");
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MasterControlSystem::new);
    }
}
