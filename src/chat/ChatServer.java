package chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 70000;
    private static Set<Socket> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("[서버 시작됨]");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                clients.add(socket);
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                while (true) {
                    String type = dis.readUTF();
                    if (type.equals("text")) {
                        String msg = dis.readUTF();
                        broadcast("text", msg);
                    } else if (type.equals("file")) {
                        String fileName = dis.readUTF();
                        long length = dis.readLong();
                        byte[] buffer = new byte[(int) length];
                        dis.readFully(buffer);
                        for (Socket s : clients) {
                            DataOutputStream out = new DataOutputStream(s.getOutputStream());
                            out.writeUTF("file");
                            out.writeUTF(fileName);
                            out.writeLong(length);
                            out.write(buffer);
                            out.flush();
                        }
                    }
                }
            } catch (IOException e) {
                clients.remove(socket);
            }
        }

        private void broadcast(String type, String msg) throws IOException {
            synchronized (clients) {
                for (Socket s : clients) {
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeUTF(type);
                    out.writeUTF(msg);
                    out.flush();
                }
            }
        }
    }
}