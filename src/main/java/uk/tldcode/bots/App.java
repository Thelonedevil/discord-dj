package uk.tldcode.bots;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Hello world!
 */
public class App {
    public static JDA jda;
    public static Set<String> playList = new LinkedHashSet<>();
    public static String folder;

    public static void main(String[] args) {
        try {
            if (args.length > 2) {
                folder = args[2];
            } else {
                folder = System.getProperty("user.home") + "/.discord-dj";
            }
            if (!new File(folder).exists()) {
                new File(folder).mkdirs();
            }
            loadSongs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //playList.forEach(System.out::println);
        System.out.println(playList.size());
        try {
            jda = new JDABuilder().setEmail(args[0]).setPassword(args[1]).addListener(new MyListenerAdapter()).buildAsync();
        } catch (LoginException e) {
            e.printStackTrace();
        }


    }

    public static void loadSongs() throws IOException {
        playList.clear();
        Files.walk(Paths.get(folder), FileVisitOption.FOLLOW_LINKS).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                File file = filePath.toFile();
                if (filePath.toString().endsWith(".mp3") || filePath.toString().endsWith(".flac"))
                    playList.add(file.getAbsolutePath());
            }
        });
    }


}
