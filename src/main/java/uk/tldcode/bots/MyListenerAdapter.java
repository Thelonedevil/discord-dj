package uk.tldcode.bots;

import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;
import org.kc7bfi.jflac.sound.spi.FlacFileFormatType;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class MyListenerAdapter extends ListenerAdapter {
    private static final ExecutorService EXECUTOR_SERVICE = ThreadUtil.newCachedThreadPool();
    private static String currentSong;
    private static State state = State.STOPPED;
    private static float volume = 0.1f;
    private static MessageChannel channel;
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
        } else if (fileFormat.getType() == FlacFileFormatType.FLAC) {
            FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
            Flac2PcmAudioInputStream flac2PcmAudioInputStream = new Flac2PcmAudioInputStream(flacAudioFileReader.getAudioInputStream(file), fileFormat.getFormat(), fileFormat.getByteLength());
            flac2PcmAudioInputStream.read();
            for (int i = 0; i < flac2PcmAudioInputStream.getMetaData().length; i++) {
                Metadata metadata = flac2PcmAudioInputStream.getMetaData()[i];
                if (metadata instanceof VorbisComment) {
                    flac2PcmAudioInputStream.close();
                    return ((VorbisComment) metadata).getCommentByName("title")[0];
                }
            }
            flac2PcmAudioInputStream.close();
            return "";
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
        } else if (fileFormat.getType() == FlacFileFormatType.FLAC) {
            FlacAudioFileReader flacAudioFileReader = new FlacAudioFileReader();
            Flac2PcmAudioInputStream flac2PcmAudioInputStream = new Flac2PcmAudioInputStream(flacAudioFileReader.getAudioInputStream(file), fileFormat.getFormat(), fileFormat.getByteLength());
            flac2PcmAudioInputStream.read();

            for (int i = 0; i < flac2PcmAudioInputStream.getMetaData().length; i++) {
                Metadata metadata = flac2PcmAudioInputStream.getMetaData()[i];

                if (metadata instanceof VorbisComment) {
                    flac2PcmAudioInputStream.close();
                    return ((VorbisComment) metadata).getCommentByName("artist")[0];
                }
            }
            flac2PcmAudioInputStream.close();
            return "";
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
        if (event.isPrivate()) {
            System.out.printf("[PM]\t%s: %s\n", event.getAuthor().getUsername(), event.getMessage().getContent());
        } else {
            System.out.printf("[%s][%s] %s: %s\n", event.getGuild().getName(),
                    event.getTextChannel(), event.getAuthor().getUsername(),
                    event.getMessage().getContent());
        }
        if (event.getMessage().getContent().split(" ")[0].equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong!");
        }
        String message = event.getMessage().getContent();
        if (message.equalsIgnoreCase("setchannel")) {
            channel = event.getChannel();
        }
        if (!event.isPrivate() && channel != null && !channel.equals(event.getChannel())) {
            return;
        }

        //Start an audio connection with a VoiceChannel
        if (message.startsWith("join ")) {
            //Separates the name of the channel so that we can search for it
            if (event.getJDA().getAudioManager().isConnected()) {
                return;
            }
            String chanName = message.substring(5);
            Guild guild = event.getGuild();

            //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
            if (event.getGuild() == null) {
                String[] parts = chanName.split(":");
                chanName = parts[1];
                guild = event.getJDA().getGuilds().stream().filter(vGuild -> vGuild.getName().equalsIgnoreCase(parts[0])).findFirst().orElse(null);
            }
            final String channelName = chanName;
            if (guild == null) {
                event.getChannel().sendMessage("There isn't a channel in the Guild:VoiceChannel with the name: '" + chanName + "'");
                return;
            }
            VoiceChannel channel = guild.getVoiceChannels().stream().filter(
                    vChan -> vChan.getName().equalsIgnoreCase(channelName))
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
        if (message.toLowerCase(Locale.ROOT).startsWith("play")) {
            if (state == State.PAUSED) {
                state = State.PLAYING;
                player.play();
            } else if (state == State.STOPPED) {
                state = State.NEXT;
            }
            channel = event.getChannel();
            playAll(message.contains("shuffle"));


        }
        if (message.equalsIgnoreCase("reload songs")) {
            try {
                App.loadSongs();
                playQueue.clear();
                playQueue.addAll(App.playList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (message.startsWith("volume ")) {
            String[] parts = message.split(" ");
            volume = (float) Integer.parseInt(parts[1]) / 100f;
            player.setVolume(volume);
        }

        if (message.equalsIgnoreCase("song?")) {
            try {
                event.getChannel().sendMessage("Current song is: " + getSongTitleWithMp3Spi(new File(currentSong)) + " - " + getArtistwithMp3Spi(new File(currentSong)));
            } catch (UnsupportedAudioFileException | IOException e) {
                e.printStackTrace();
            }
        }

        //You can't pause, stop or restart before a player has even been created!
        if (player == null && (message.equalsIgnoreCase("pause") || message.equalsIgnoreCase("stop") || message.equalsIgnoreCase("restart"))) {
            event.getChannel().sendMessage("You need to 'play' before you can preform that command.");
            return;
        }
        if (player != null) {
            if (message.equalsIgnoreCase("pause")) {
                state = State.PAUSED;
                channel = event.getChannel();
                player.pause();
            }
            if (message.equalsIgnoreCase("stop")) {
                state = State.STOPPED;
                channel = event.getChannel();
                player.stop();

            }
            if (message.equalsIgnoreCase("restart")) {
                state = State.PLAYING;
                channel = event.getChannel();
                player.restart();
            }
            if (message.equalsIgnoreCase("skip")) {
                state = State.NEXT;
                channel = event.getChannel();
            }
        }
    }

    public void playAll(boolean shuffle) {
        if (playQueue.size() > 0) {
            return;
        }
        List<String> list = new ArrayList<>(App.playList);
        if (shuffle) {
            Collections.shuffle(list);
        }
        playQueue.addAll(list);

        EXECUTOR_SERVICE.submit(() -> playFiles(shuffle));

    }

    public void playFiles(boolean shuffle) {
        try {
            String file;
            while (true) {
                if (state == State.NEXT) {
                    if (playQueue.size() == 0) {
                        playAll(shuffle);
                        return;
                    }
                    file = playQueue.remove();
                    currentSong = file;
                    try {
                        App.jda.getAccountManager().setGame(getSongTitleWithMp3Spi(new File(currentSong)));
                        channel.sendMessage("Now Playing: " + getSongTitleWithMp3Spi(new File(currentSong)) + " - " + getArtistwithMp3Spi(new File(currentSong)));
                    } catch (UnsupportedAudioFileException e) {
                        e.printStackTrace();
                    }
                    state = State.PLAYING;
                    playFile(file);
                }
                if (player.isStopped() && state != State.STOPPED) {
                    state = State.NEXT;
                }
                Thread.sleep(200);

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playFile(String file) {
        File audioFile = null;
        URL audioUrl = null;
        try {
            audioFile = new File(file);
            player = new FilePlayer(audioFile);
            player.setVolume(volume);
            App.jda.getAudioManager().setSendingHandler(player);
            player.play();
        } catch (IOException e) {
            channel.sendMessage("Could not load the file. Does it exist?  File name: " + audioFile.getName());
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            channel.sendMessage("Could not load file. It either isn't an audio file or isn't a" +
                    " recognized audio format.");
            e.printStackTrace();
        }
        if (player.isStarted() && player.isStopped()) { //If it did exist, has it been stop()'d before?
            channel.sendMessage("The player has been stopped. To start playback, please use 'restart'");
        } else {
            player.play();
        }
    }
}
