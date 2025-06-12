package chat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ChatUtils {
    private static final String chatPath = "C:\\dev\\chat";
    private static final String USER_PROPS = chatPath + File.separator +"user.properties";

    public static String getUserName() {
        File propFile = new File(USER_PROPS);
        if (!propFile.exists()) {
            return null;
        }
        try (InputStream input = new FileInputStream(propFile)) {
            Properties prop = new Properties();
            prop.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return prop.getProperty("username");
        } catch (IOException e) {
            return null;
        }
    }

    public static void saveUserName(String username) {
        try {
            File dir = new File(chatPath);
            if (!dir.exists()) dir.mkdirs();
            try (OutputStream output = new FileOutputStream(USER_PROPS)) {
                Properties prop = new Properties();
                prop.setProperty("username", username);
                prop.store(new OutputStreamWriter(output, StandardCharsets.UTF_8), null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveChat(String message) {
        try {
            File file = new File(chatPath + File.separator + "chat_history.txt");
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                writer.write(message);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadChat() {
        File file = new File(chatPath + File.separator + "chat_history.txt");
        if (!file.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}