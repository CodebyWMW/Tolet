package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {
        try {
            // 🔴 Replace with your server IP when testing on different PC
            Socket socket = new Socket("localhost", 5000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            // 🔐 LOGIN first
            out.println("LOGIN|user1|1234");
            System.out.println(in.readLine());

            // 📥 GET ALL HOUSES
            out.println("GET_ALL_HOUSES");

            String response;
            while (!(response = in.readLine()).equals("END")) {
                System.out.println("House: " + response);
            }

            // ➕ ADD HOUSE
            out.println("ADD_HOUSE|Dhaka|12000");
            System.out.println(in.readLine());

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}