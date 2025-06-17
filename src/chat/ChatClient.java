package chat;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    private static String HOST = "192.168.10.202";
    private static final int PORT = 12345;
    private JTextArea chatArea;
    private JTextArea inputArea;
    private JButton sendBtn, fileBtn;
    private Socket socket;
    private DataOutputStream dos;
    private String username;

    public static void main(String[] args) {
//        Object input = JOptionPane.showInputDialog(null, "서버 IP를 입력하세요:", "서버 접속", JOptionPane.QUESTION_MESSAGE, null, null, "192.168.10.202");
//        HOST = (input == null || input.toString().trim().isEmpty()) ? "localhost" : input.toString().trim();
        SwingUtilities.invokeLater(ChatClient::new);
    }

    public ChatClient() {
        username = ChatUtils.getUserName();
        if (username == null) {
            username = JOptionPane.showInputDialog(null, "사용자 이름을 입력하세요:", "이름 설정", JOptionPane.PLAIN_MESSAGE);
            if (username == null || username.trim().isEmpty()) {
                System.exit(0);
            }
            ChatUtils.saveUserName(username);
        }
        setupUI();
        connect();
    }

    private void setupUI() {
        JFrame frame = new JFrame("Java 채팅 - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        ((DefaultCaret) chatArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = chatArea.viewToModel2D(e.getPoint());
                try {
                    int line = chatArea.getLineOfOffset(offset);
                    String selectedLine = chatArea.getText(chatArea.getLineStartOffset(line), chatArea.getLineEndOffset(line) - chatArea.getLineStartOffset(line)).trim();
                    if (selectedLine.startsWith("[파일 수신됨:")) {
                        String fileName = selectedLine.substring(selectedLine.indexOf(":") + 1, selectedLine.lastIndexOf("]")).trim();
                        JFileChooser chooser = new JFileChooser();
                        chooser.setSelectedFile(new File(fileName));
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            File destFile = chooser.getSelectedFile();
                            // 서버에 파일 다운로드 요청
                            requestFileDownload(fileName, destFile);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputArea = new JTextArea(3, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        JScrollPane inputScroll = new JScrollPane(inputArea);

        sendBtn = new JButton("전송");
        sendBtn.addActionListener(e -> sendMessage());

        fileBtn = new JButton("파일 전송");
        fileBtn.addActionListener(e -> sendFile());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendBtn);
        buttonPanel.add(fileBtn);
        bottomPanel.add(inputScroll, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void connect() {
        try {
            socket = new Socket(HOST, PORT);
            dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String type = dis.readUTF();
                        if (type.equals("text")) {
                            String msg = dis.readUTF();
                            chatArea.append(msg + "\n");
                        } else if (type.equals("file")) {
                            String fileName = dis.readUTF();
                            long length = dis.readLong();
                            byte[] buffer = new byte[(int) length];
                            dis.readFully(buffer);

                            String msg = "[파일 수신됨: " + fileName + "]";
                            chatArea.append(msg + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "서버와의 연결이 끊어졌습니다.");
                    System.exit(1);
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "서버 연결 실패", "오류", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void sendMessage() {
        try {
            String text = inputArea.getText().trim();
            if (text.isEmpty()) return;
            String msg = username + ": " + text;
            dos.writeUTF("text");
            dos.writeUTF(msg);
            dos.flush();
            inputArea.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] data = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                }

                dos.writeUTF("file");
                dos.writeUTF(file.getName());
                dos.writeLong(data.length);
                dos.write(data);
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "파일 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void requestFileDownload(String fileName, File destFile) {
        // 향후 서버에서 파일 다운로드 기능 구현 시 사용할 메서드
        try {
            dos.writeUTF("download");
            dos.writeUTF(fileName);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "파일 다운로드 요청 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}