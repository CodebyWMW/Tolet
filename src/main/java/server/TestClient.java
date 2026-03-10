package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            // Send test command
            out.println("GET_ALL_HOUSES");

            // Read server response
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Server says: " + response);
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}