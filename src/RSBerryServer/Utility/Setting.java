package RSBerryServer.Utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Setting
{
    private static boolean loaded = false;
    private static Map<String, String> settings = new HashMap<String, String>();

    public static String get(String key)
    {
        if (!loaded) {
            try (BufferedReader br = new BufferedReader(new FileReader("settings.rsb"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(" ");
                    settings.put(parts[0], parts[1]);
                }
                loaded = true;
            } catch (IOException ioe) {
                System.out.println("Could not open file reading.");
                System.exit(1);
            }
        }
        if (!settings.containsKey(key)) {
            return "Missing Value";
        }
        return settings.get(key);
    }

    public static int getMaxPlayers()
    {
        return Integer.parseInt(get("MaxPlayers"));
    }

    public static int getPort()
    {
        return Integer.parseInt(get("ServerPort"));
    }
}
