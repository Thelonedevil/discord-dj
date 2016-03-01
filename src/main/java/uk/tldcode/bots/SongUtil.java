package uk.tldcode.bots;

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
import java.util.Map;

public class SongUtil {
    public static int getDurationWithMp3Spi(File file) throws UnsupportedAudioFileException, IOException {
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

    public static String getSongTitleWithMp3Spi(File file) throws UnsupportedAudioFileException, IOException {
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

    public static String getArtistwithMp3Spi(File file) throws IOException, UnsupportedAudioFileException {
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
}
