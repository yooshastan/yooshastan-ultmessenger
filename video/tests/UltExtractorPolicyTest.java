import com.yooshastan.ultmessenger.video.UltExtractorPolicy;
import java.io.*;
import java.nio.file.*;
public final class UltExtractorPolicyTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        String h = "a".repeat(64);
        check(UltExtractorPolicy.checksum(h+"  yt-dlp\n"+"b".repeat(64)+"  yt-dlp.exe\n").equals(h));
        check(UltExtractorPolicy.checksum(h+" *yt-dlp\r\n").equals(h));
        try { UltExtractorPolicy.checksum(h+"  yt-dlp\n"+h+"  yt-dlp\n"); throw new AssertionError(); } catch(IOException expected) {}
        try { UltExtractorPolicy.checksum(h+"  yt-dlp.exe\n"); throw new AssertionError(); } catch(IOException expected) {}
        check(UltExtractorPolicy.validVersion("2026.11.24.232653"));
        check(!UltExtractorPolicy.validVersion("../bad"));
        check(!UltExtractorPolicy.meetsBaseline("2026.10.26"));
        check(!UltExtractorPolicy.meetsBaseline("2026.11.22.999"));
        check(UltExtractorPolicy.meetsBaseline("2026.11.23"));
        check(UltExtractorPolicy.meetsBaseline("2026.11.23.232705"));
        check(UltExtractorPolicy.meetsBaseline("2027.1.1"));
        check(!UltExtractorPolicy.meetsBaseline("Unknown"));
        check(UltExtractorPolicy.asset(false,"2026.11.23","yt-dlp").equals("https://github.com/yt-dlp/yt-dlp/releases/download/2026.11.23/yt-dlp"));
        File d=Files.createTempDirectory("ult-extractor-test").toFile();
        File live=new File(d,"live"), stage=new File(d,"stage"), backup=new File(d,"backup"), tmp=new File(d,"tmp");
        try {
            Files.writeString(live.toPath(),"working"); Files.writeString(stage.toPath(),"new");
            try { UltExtractorPolicy.replace(stage,live,backup,tmp,h); throw new AssertionError(); } catch(IOException expected) {}
            check(Files.readString(live.toPath()).equals("working"));
            UltExtractorPolicy.replace(stage,live,backup,tmp,UltExtractorPolicy.sha256(stage));
            check(Files.readString(live.toPath()).equals("new"));
            check(Files.readString(backup.toPath()).equals("working"));
            Files.copy(backup.toPath(),stage.toPath());
            UltExtractorPolicy.replace(stage,live,backup,tmp,UltExtractorPolicy.sha256(stage));
            check(Files.readString(live.toPath()).equals("working"));
            check(Files.readString(backup.toPath()).equals("new"));
            try { UltExtractorPolicy.copy(new ByteArrayInputStream(new byte[5]),stage,4); throw new AssertionError(); } catch(IOException expected) {}
        } finally { for (File f:d.listFiles()) f.delete(); d.delete(); }
        System.out.println("UltExtractorPolicyTest OK");
    }
}
