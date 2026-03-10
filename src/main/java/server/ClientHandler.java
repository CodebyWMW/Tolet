package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import dao.HouseDAO;
import models.House;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);
        ) {

            String command = in.readLine();

            if (command.equals("GET_ALL_HOUSES")) {

                HouseDAO houseDAO = new HouseDAO();
                List<House> houses = houseDAO.getAllHouses();

                if (houses.isEmpty()) {
                    out.println("NO_HOUSES");
                    return;
                }

                for (House h : houses) {
                    out.println(h.getId() + " " + h.getLocation());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}