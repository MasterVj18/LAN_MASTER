package client;

import java.io.*;
import java.net.*;
import javax.swing.*;

public class ClientAgent {
    public static void main(String[] args) {
        try {
            System.out.println("Attempting to connect to server at localhost:9090...");
            Socket socket = new Socket("localhost", 9090);
            System.out.println("Connection established.");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String host = InetAddress.getLocalHost().getHostName();
            String ip = InetAddress.getLocalHost().getHostAddress();
            String mac = getMac();

            out.println(host + " | " + ip + " | " + mac);

            new Thread(new CommandListener(socket)).start();
        } catch (Exception e) {
            e.printStackTrace();
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
}

