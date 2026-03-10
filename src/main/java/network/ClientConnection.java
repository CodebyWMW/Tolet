package network;


import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClientConnection {

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void connect() {

        try {

            socket = new Socket("localhost", 5000);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected to server");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ObjectOutputStream getOut() {
        return out;
    }

    public static ObjectInputStream getIn() {
        return in;
    }
}