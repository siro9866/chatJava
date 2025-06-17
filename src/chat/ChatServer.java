package chat;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final String CHAT_PROPS = System.getProperty("user.home") + File.separator + "chat" + File.separator;
    private static final String SERVER_STORAGE_PATH = CHAT_PROPS + "server_storage";
    private static final String CHAT_HISTORY_FILE = "chat_history.txt";
    private static final String FILES_DIR = "files";
    private static Set<Socket> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("[서버 시작됨]");
        initializeServerStorage();
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

    private static void initializeServerStorage() {
        try {
            // 서버 저장소 디렉토리 생성
            Files.createDirectories(Paths.get(SERVER_STORAGE_PATH));
            Files.createDirectories(Paths.get(SERVER_STORAGE_PATH, FILES_DIR));
            
            // 채팅 히스토리 파일이 없으면 생성
            Path chatHistoryPath = Paths.get(SERVER_STORAGE_PATH, CHAT_HISTORY_FILE);
            if (!Files.exists(chatHistoryPath)) {
                Files.createFile(chatHistoryPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveChatMessage(String message) {
        Path chatHistoryPath = Paths.get(SERVER_STORAGE_PATH, CHAT_HISTORY_FILE);
        try {
            Files.write(chatHistoryPath, 
                       (message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                       StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String loadChatHistory() {
        Path chatHistoryPath = Paths.get(SERVER_STORAGE_PATH, CHAT_HISTORY_FILE);
        try {
            return new String(Files.readAllBytes(chatHistoryPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
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
                
                // 연결 직후 채팅 히스토리 전송
                String chatHistory = loadChatHistory();
                dos.writeUTF("text");
                dos.writeUTF(chatHistory);
                dos.flush();

                while (true) {
                    String type = dis.readUTF();
                    if (type.equals("text")) {
                        String msg = dis.readUTF();
                        saveChatMessage(msg);
                        broadcast("text", msg);
                    } else if (type.equals("file")) {
                        String fileName = dis.readUTF();
                        long length = dis.readLong();
                        byte[] buffer = new byte[(int) length];
                        dis.readFully(buffer);

                        // 서버에 파일 저장
                        Path filePath = Paths.get(SERVER_STORAGE_PATH, FILES_DIR, fileName);
                        Files.write(filePath, buffer);

                        // 모든 클라이언트에게 파일 전송
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