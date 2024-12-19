import javax.sound.sampled.*;
import java.io.File;

public class BackgroundMusic {
    private static Clip clip;

    public static void play() {
        new Thread(() -> {
            try {
                File audioFile = new File("./mp3/bg.wav"); // 背景音樂檔案
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                clip = AudioSystem.getClip();
                clip.open(audioStream);

                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(volumeControl, 0.3f);

                clip.start(); // 撥放音樂
                clip.loop(Clip.LOOP_CONTINUOUSLY); // 重複播放
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop(); // 停止音樂
        }
    }

    private static void setVolume(FloatControl volumeControl, float volume) {
        float min = volumeControl.getMinimum();
        float max = volumeControl.getMaximum();
        float db = min + (max - min) * volume; // 計算分貝值
        volumeControl.setValue(db);
    }
}