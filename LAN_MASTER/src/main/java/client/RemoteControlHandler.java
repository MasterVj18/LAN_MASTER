package client;

import java.net.Socket;

public class RemoteControlHandler {
    private static Thread screenShareThread;
    private static boolean running = false;

    public static void startControl(Socket socket) {
        running = true;
        screenShareThread = new Thread(() -> {
            while (running) {
                // send screen frames, listen to input
            }
        });
        screenShareThread.start();
    }

    public static void stopControl() {
        running = false;
        if (screenShareThread != null && screenShareThread.isAlive()) {
            screenShareThread.interrupt();
        }
        System.out.println("Remote control stopped.");
    }
}

