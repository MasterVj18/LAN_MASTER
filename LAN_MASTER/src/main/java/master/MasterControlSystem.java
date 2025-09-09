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
    private JButton btnStartServer, btnShutdown, btnRestart, btnSendFile, btnBroadcast,btnRemoteControl,stopRemoteControlButton;;
    private JButton btnStartStream, btnStopStream, btnStartWebcam, btnStopWebcam;
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

        // Buttons
        btnStartServer = new JButton("Start Server");
        btnShutdown = new JButton("Shutdown");
        btnRestart = new JButton("Restart");
        btnSendFile = new JButton("Send File");
        btnBroadcast = new JButton("Broadcast Message");
        btnStartStream = new JButton("Start Screen Stream");
        btnStopStream = new JButton("Stop Screen Stream");
        btnStartWebcam = new JButton("Start Webcam");
        btnStopWebcam = new JButton("Stop Webcam");
        btnRemoteControl = new JButton("Remote Control");
        stopRemoteControlButton = new JButton("Stop Remote Control");


        // Layouts
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Connected Devices"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(deviceList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(10, 1));
        buttonPanel.add(btnStartServer);
        buttonPanel.add(btnShutdown);
        buttonPanel.add(btnRestart);
        buttonPanel.add(btnSendFile);
        buttonPanel.add(btnBroadcast);
        buttonPanel.add(btnStartStream);
        buttonPanel.add(btnStopStream);
        buttonPanel.add(btnStartWebcam);
        buttonPanel.add(btnStopWebcam);
        buttonPanel.add(btnRemoteControl);
        buttonPanel.add(stopRemoteControlButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.EAST);
        add(imageLabel, BorderLayout.CENTER);

        // Event listeners
        btnStartServer.addActionListener(e -> {
            startServer();
            startImageServer();
            startWebcamServer();
        });
        btnShutdown.addActionListener(e -> sendCommand("SHUTDOWN"));
        btnRestart.addActionListener(e -> sendCommand("RESTART"));
        stopRemoteControlButton.addActionListener(e -> sendCommand("STOP_REMOTE_CONTROL"));
        btnBroadcast.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog("Enter message:");
            if (msg != null && !msg.isEmpty()) {
                sendCommand("BROADCAST:" + msg);
            }
        });
        btnRemoteControl.addActionListener(e -> {
            sendCommand("START_REMOTE_CONTROL"); // Ask client to start server

            String ip = getSelectedClientIP();
            if (ip != null) {
                new Thread(() -> {
                    try {
                        master.control.RemoteControllerClient viewer = new master.control.RemoteControllerClient(ip);
                        viewer.setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log("Error launching remote viewer: " + ex.getMessage());
                    }
                }).start();
            } else {
                log("Could not extract client IP.");
            }
        });


        btnStartStream.addActionListener(e -> startScreenStream());
        btnStopStream.addActionListener(e -> stopScreenStream());
        btnStartWebcam.addActionListener(e -> startWebcamStream());
        btnStopWebcam.addActionListener(e -> stopWebcamStream());

        setVisible(true);
    }
    private String getSelectedClientIP() {
        String selected = deviceList.getSelectedValue();
        if (selected == null) return null;

        // Expected format: "Host | IP | MAC"
        String[] parts = selected.split("\\|");
        return parts.length >= 2 ? parts[1].trim() : null;
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
        try (DataInputStream dis = new DataInputStream(imageSocket.getInputStream())) {
            while (streaming) {
                int imageLength = dis.readInt();
                byte[] imageData = new byte[imageLength];
                dis.readFully(imageData);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                if (image != null) {
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(800, 600, Image.SCALE_SMOOTH));
                    SwingUtilities.invokeLater(() -> imageLabel.setIcon(icon));
                }
            }
        } catch (IOException e) {
            log("Streaming stopped: " + e.getMessage());
        }
    }

    private void startWebcamServer() {
        new Thread(() -> {
            try (ServerSocket webcamServerSocket = new ServerSocket(9092)) {
                log("Webcam server started on port 9092");
                while (true) {
                    Socket webcamSocket = webcamServerSocket.accept();
                    new Thread(() -> handleWebcamSocket(webcamSocket)).start();
                }
            } catch (IOException e) {
                log("Webcam server error: " + e.getMessage());
            }
        }).start();
    }

    private void handleWebcamSocket(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            while (streaming) {
                int length = dis.readInt();
                byte[] data = new byte[length];
                dis.readFully(data);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                if (image != null) {
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(800, 600, Image.SCALE_SMOOTH));
                    SwingUtilities.invokeLater(() -> imageLabel.setIcon(icon));
                }
            }
        } catch (IOException e) {
            log("Webcam streaming stopped: " + e.getMessage());
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

    private void startWebcamStream() {
        streaming = true;
        sendCommand("START_WEBCAM_STREAM");
        log("Requested client to start webcam stream...");
    }

    private void stopWebcamStream() {
        streaming = false;
        sendCommand("STOP_WEBCAM_STREAM");
        log("Requested client to stop webcam stream...");
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MasterControlSystem::new);
    }
}
