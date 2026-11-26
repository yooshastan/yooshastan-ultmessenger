import com.yooshastan.ultmessenger.video.UltMediaOptions;
public final class UltMediaOptionsTest {
    static void check(boolean b) { if (!b) throw new AssertionError(); }
    public static void main(String[] args) {
        UltMediaOptions video = new UltMediaOptions(true, false, 720, "bad");
        check(video.manual && !video.audio && video.height == 720 && video.variant().equals("video-720"));
        check(new UltMediaOptions(false, true, 123, "bad").audioFormat.equals("m4a"));
        check(new UltMediaOptions(true, true, 0, "mp3").mime().equals("audio/mpeg"));
        check(new UltMediaOptions(true, true, 0, "opus").mime().equals("audio/ogg"));
        check(new UltMediaOptions(true, false, 720, "m4a", true).variant().equals("video-720"));
        check(new UltMediaOptions(true, false, 720, "m4a", true).directVideoPost());
        check(!new UltMediaOptions(true, true, 0, "m4a", true).directVideoPost());
        check(!new UltMediaOptions(false, false, 720, "m4a", true).toChat);
        for (String format : new String[]{"m4a","mp3","opus"}) {
            UltMediaOptions chat = new UltMediaOptions(true, true, 0, format, true);
            check(chat.directChatPost() && chat.extension().equals(format));
            check(chat.handoffVariant().equals(format));
            check(chat.variant().equals(format + "-chat"));
            check(!new UltMediaOptions(true, true, 0, format).directChatPost());
        }
        check(video.handoffVariant().equals("video"));
        System.out.println("UltMediaOptionsTest OK");
    }
}
