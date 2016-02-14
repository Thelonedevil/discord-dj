package uk.tldcode.bots;

import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class MyListenerAdapter extends ListenerAdapter {
    private static final ExecutorService EXECUTOR_SERVICE = ThreadUtil.newCachedThreadPool();
    private static String currentSong;
    private static State state;
    private static int timeTillNextSong;
    Player player = null;
    Queue<String> playQueue = new LinkedBlockingQueue<>();

    private static int getDurationWithMp3Spi(File file) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        if (fileFormat instanceof TAudioFileFormat) {
            Map<?, ?> properties = fileFormat.properties();
            String key = "duration";
            Long microseconds = (Long) properties.get(key);
            int mili = (int) (microseconds / 1000);
            int sec = (mili / 1000) % 60;
            int min = (mili / 1000) / 60;
            System.out.println("time = " + min + ":" + sec);
            return mili;
        } else {
            throw new UnsupportedAudioFileException();
        }
    }

    private static String getSongTitleWithMp3Spi(File file) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        if (fileFormat instanceof TAudioFileFormat) {
            Map<?, ?> properties = fileFormat.properties();
            String key = "title";
            return (String) properties.get(key);
        } else {
            throw new UnsupportedAudioFileException();
        }
    }

    private static String getArtistwithMp3Spi(File file) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        if (fileFormat instanceof TAudioFileFormat) {
            Map<?, ?> properties = fileFormat.properties();
            String key = "author";
            return (String) properties.get(key);
        } else {
            throw new UnsupportedAudioFileException();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getAccountManager().setGame("Bot Simulator 2016");
        event.getJDA().getGuilds().forEach((g) -> System.out.println(g.getName()));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        System.out.printf("[%s][%s] %s: %s\n", event.getGuild().getName(),
                event.getTextChannel(), event.getAuthor().getUsername(),
                event.getMessage().getContent());

        if (event.getMessage().getContent().split(" ")[0].equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong!");
        }
        String message = event.getMessage().getContent();

        //Start an audio connection with a VoiceChannel
        if (message.startsWith("join ")) {
            //Separates the name of the channel so that we can search for it
            String chanName = message.substring(5);

            //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
            VoiceChannel channel = event.getGuild().getVoiceChannels().stream().filter(
                    vChan -> vChan.getName().equalsIgnoreCase(chanName))
                    .findFirst().orElse(null);  //If there isn't a matching name, return null.
            if (channel == null) {
                event.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: '" + chanName + "'");
                return;
            }
            event.getJDA().getAudioManager().openAudioConnection(channel);
        }
        //Disconnect the audio connection with the VoiceChannel.
        if (message.equals("leave"))
            event.getJDA().getAudioManager().closeAudioConnection();

        //Start playing audio with our FilePlayer. If we haven't created and registered a FilePlayer yet, do that.
        if (message.startsWith("play")) {
            state = state == State.PAUSED ? State.PLAYING : State.NEXT;
            if (state == State.PLAYING) {
                player.play();
            }
            playAll(event, message.contains("shuffle"));


        }
        if (message.equalsIgnoreCase("reload songs")) {
            try {
                Files.walk(Paths.get(System.getProperty("user.home") + "/.discord-dj")).forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        File file = filePath.toFile();
                        App.playList.add(file.getAbsolutePath());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (message.startsWith("volume ")) {
            String[] parts = message.split(" ");
            player.setVolume((float) Integer.parseInt(parts[1]) / 100f);
        }

        if (message.equalsIgnoreCase("song?")) {
            try {
                event.getChannel().sendMessage("Current song is: " + getSongTitleWithMp3Spi(new File(currentSong)) + " - " + getArtistwithMp3Spi(new File(currentSong)));
            } catch (UnsupportedAudioFileException | IOException e) {
                e.printStackTrace();
            }
        }
        //You can't pause, stop or restart before a player has even been created!
        if (player == null && (message.equals("pause") || message.equals("stop") || message.equals("restart"))) {
            event.getChannel().sendMessage("You need to 'play' before you can preform that command.");
            return;
        }
        if (player != null) {
            if (message.equals("pause")) {
                state = State.PAUSED;
                player.pause();
            }
            if (message.equals("stop")) {
                state = State.STOPPED;
                player.stop();

            }
            if (message.equals("restart")) {
                state = State.PLAYING;
                player.restart();
            }
            if (message.equals("skip")) {
                state = State.NEXT;
            }
        }
    }

    public void playAll(MessageReceivedEvent event, boolean shuffle) {
        if (playQueue.size() > 0) {
            return;
        }
        List<String> list = new ArrayList<>(App.playList);
        if (shuffle) {
            Collections.shuffle(list);
        }
        playQueue.addAll(list);

        EXECUTOR_SERVICE.submit(() -> playFiles(event, shuffle));

    }

    public void playFiles(MessageReceivedEvent event, boolean shuffle) {
        try {
            String file;
            while (true) {
                if (state == State.NEXT) {
                    if (playQueue.size() == 0) {
                        playAll(event, shuffle);
                        return;
                    }
                    file = playQueue.remove();
                    playFile(event, file);
                    currentSong = file;
                    state = State.PLAYING;
                }
                if (player.isStopped() && state != State.STOPPED) {
                    state = State.NEXT;
                }
                Thread.sleep(200);

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void playFile(MessageReceivedEvent event, String file) {
        File audioFile = null;
        URL audioUrl = null;
        try {
            audioFile = new File(file);
            player = new FilePlayer(audioFile);
            event.getJDA().getAudioManager().setSendingHandler(player);
            player.play();
        } catch (IOException e) {
            event.getChannel().sendMessage("Could not load the file. Does it exist?  File name: " + audioFile.getName());
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            event.getChannel().sendMessage("Could not load file. It either isn't an audio file or isn't a" +
                    " recognized audio format.");
            e.printStackTrace();
        }
        if (player.isStarted() && player.isStopped()) { //If it did exist, has it been stop()'d before?
            event.getChannel().sendMessage("The player has been stopped. To start playback, please use 'restart'");
        } else {
            player.play();
        }
    }
}
