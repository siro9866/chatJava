package chat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ChatUtils {
    private static final String USER_PROPS = System.getProperty("user.home") + File.separator + "chat" + File.separator + ".chat_config";

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
            File propFile = new File(USER_PROPS);
            propFile.getParentFile().mkdirs();
            try (OutputStream output = new FileOutputStream(propFile)) {
                Properties prop = new Properties();
                prop.setProperty("username", username);
                prop.store(new OutputStreamWriter(output, StandardCharsets.UTF_8), null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}