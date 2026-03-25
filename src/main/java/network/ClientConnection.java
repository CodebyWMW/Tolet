package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientConnection {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static synchronized void connect() {
        if (isConnected()) {
            return;
        }

        try {

            socket = new Socket("localhost", 5000);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server");

        } catch (IOException e) {
            throw new IllegalStateException("Unable to connect to server", e);
        }
    }

    public static synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public static synchronized String sendCommand(String command) throws IOException {
        connect();
        System.out.println("[ClientConnection] Sending: " + (command.contains("SIGNUP") ? 
                          command.substring(0, Math.min(50, command.length())) + "..." : command));
        out.println(command);
        String response = in.readLine();
        System.out.println("[ClientConnection] Received: " + response);
        return response;
    }

    public static synchronized List<String> sendCommandForLines(String command, String endMarker) throws IOException {
        connect();
        out.println(command);

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if (endMarker.equals(line)) {
                break;
            }
            lines.add(line);
        }
        return lines;
    }

    public static synchronized void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }

        if (out != null) {
            out.close();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        in = null;
        out = null;
        socket = null;
    }
}