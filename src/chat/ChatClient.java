package chat;

// ===== 2. 클라이언트 UI 및 네트워크: ChatClient.java =====
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ChatClient {
    private static String HOST = "localhost"; // 서버 IP
    private static final int PORT = 70000;
    private final String filePath = "C:\\dev\\chat\\files";
    private JTextArea chatArea;
    private JTextArea inputArea;
    private JButton sendBtn, fileBtn;
    private Socket socket;
    private DataOutputStream dos;
    private String username;
    private File lastSentFile;

    public static void main(String[] args) {
        Object input = JOptionPane.showInputDialog(null, "서버 IP를 입력하세요:", "서버 접속", JOptionPane.QUESTION_MESSAGE, null, null, "192.168.10.202");
        HOST = (input == null || input.toString().trim().isEmpty()) ? "192.168.10.202" : input.toString().trim();
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
        chatArea.setText(ChatUtils.loadChat());

        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = chatArea.viewToModel2D(e.getPoint());
                try {
                    int line = chatArea.getLineOfOffset(offset);
                    String selectedLine = chatArea.getText(chatArea.getLineStartOffset(line), chatArea.getLineEndOffset(line) - chatArea.getLineStartOffset(line)).trim();
                    if (selectedLine.startsWith("[파일 수신됨:")) {
                        String fileName = selectedLine.substring(selectedLine.indexOf(":") + 1, selectedLine.lastIndexOf("]")).trim();
                        File sourceFile = new File(filePath + File.separator + fileName);
                        if (sourceFile.exists()) {
                            JFileChooser chooser = new JFileChooser();
                            chooser.setSelectedFile(new File(fileName));
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                File destFile = chooser.getSelectedFile();
                                try {
                                    java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    JOptionPane.showMessageDialog(null, "파일이 저장되었습니다: " + destFile.getAbsolutePath());
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                    JOptionPane.showMessageDialog(null, "파일 저장 중 오류가 발생했습니다.");
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "파일을 찾을 수 없습니다.");
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

//        downloadBtn = new JButton("내가 보낸 파일 저장");
//        downloadBtn.addActionListener(e -> downloadMyFile());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendBtn);
        buttonPanel.add(fileBtn);
//        buttonPanel.add(downloadBtn);
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
                            ChatUtils.saveChat(msg);
                        } else if (type.equals("file")) {
                            String fileName = dis.readUTF();
                            long length = dis.readLong();
                            byte[] buffer = new byte[(int) length];
                            dis.readFully(buffer);
                            File file = new File(filePath);
                            file.mkdirs();
                            FileOutputStream fos = new FileOutputStream(new File(file, fileName));
                            fos.write(buffer);
                            fos.close();
                            String msg = "[파일 수신됨: " + fileName + "]";
                            chatArea.append(msg + "\n");
                            ChatUtils.saveChat(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
            lastSentFile = file;
            try {
                byte[] data = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(data);
                fis.close();

                dos.writeUTF("file");
                dos.writeUTF(file.getName());
                dos.writeLong(data.length);
                dos.write(data);
                dos.flush();

                String msg = "[파일 전송됨: " + file.getName() + "]";
                chatArea.append(msg + "\n");
                ChatUtils.saveChat(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadMyFile() {
        if (lastSentFile == null) {
            JOptionPane.showMessageDialog(null, "보낸 파일이 없습니다.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(lastSentFile.getName()));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                Files.copy(lastSentFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(null, "파일이 저장되었습니다: " + dest.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "파일 저장 중 오류가 발생했습니다.");
            }
        }
    }
}
