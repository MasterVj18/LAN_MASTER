package client;

import common.Command;

import java.net.Socket;
import java.io.*;
import java.net.*;

public class CommandListener implements Runnable {
    private Socket socket;

    public CommandListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String command;
            while ((command = in.readLine()) != null) {
                switch (command) {
                    case Command.SHUTDOWN:
                        Runtime.getRuntime().exec("shutdown -s -t 0");
                        break;
                    case Command.START_STREAM:
                        ScreenCapture.startScreenStreaming();
                        break;
                    case Command.STOP_STREAM:
                        ScreenCapture.stopScreenStreaming();
                        break;
                    case Command.START_WEBCAM_STREAM:
                        WebcamStreamer.startStreaming("localhost", 9092);
                        break;
                    case Command.STOP_WEBCAM_STREAM:
                        WebcamStreamer.stopStreaming();
                        break;
                    case Command.START_REMOTE_CONTROL:
                        new Thread(() -> {
                            try {
                                mousemove.RemoteControlServer.main(null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                        break;
                    case Command.STOP_REMOTE_CONTROL:
                        RemoteControlHandler.stopControl();
                        break;




                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

