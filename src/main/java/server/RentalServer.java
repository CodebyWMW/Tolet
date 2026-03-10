package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RentalServer {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        System.out.println("Rental Server Starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Create a new thread for each client
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.out.println("Server error:");
            e.printStackTrace();
        }
    }
}