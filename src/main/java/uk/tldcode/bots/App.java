package uk.tldcode.bots;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
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

    public static void main(String[] args) {
        try {
            Files.walk(Paths.get(System.getProperty("user.home") + "/.discord-dj")).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    File file = filePath.toFile();
                    playList.add(file.getAbsolutePath());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        playList.forEach(System.out::println);
        try {
            jda = new JDABuilder().setEmail(args[0]).setPassword(args[1]).addListener(new MyListenerAdapter()).buildAsync();
        } catch (LoginException e) {
            e.printStackTrace();
        }


    }


}
