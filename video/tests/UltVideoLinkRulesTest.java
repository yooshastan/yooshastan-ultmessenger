import com.yooshastan.ultmessenger.video.UltVideoLinkRules;
import java.util.List;

public final class UltVideoLinkRulesTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }

    public static void main(String[] args) {
        List<String> links = UltVideoLinkRules.findUrls(
                "Watch https://cdn.example.org/test.mp4?x=1. then https://youtu.be/abcd");
        check(links.size() == 2);
        check(links.get(0).equals("https://cdn.example.org/test.mp4?x=1"));
        check(UltVideoLinkRules.hasVideoExtension(links.get(0)));
        check(!UltVideoLinkRules.isAllowedUrl("http://example.com/a.mp4"));
        check(!UltVideoLinkRules.isAllowedUrl("https://127.0.0.1/a.mp4"));
        check(!UltVideoLinkRules.isAllowedUrl("https://router.lan/a.mp4"));
        check(!UltVideoLinkRules.isAllowedUrl("https://user:password@example.org/a.mp4"));
        check(!UltVideoLinkRules.isAllowedUrl("https://example.org:8096/a.mp4"));
        check(UltVideoLinkRules.isAllowedUrl("https://media.example.org/a.mp4"));
        check(UltVideoLinkRules.isSiteUrl("https://youtu.be/3-B6fmO08UY"));
        check("YouTube".equals(UltVideoLinkRules.sourceCategory("https://youtu.be/3-B6fmO08UY")));
        check("Instagram".equals(UltVideoLinkRules.sourceCategory(
                "https://www.instagram.com/reel/DelvqtyyXy9/?avpt=aWhiYWY3ZmFwbTJ0")));
        check("Other HTTPS webpage (metadata only)".equals(
                UltVideoLinkRules.sourceCategory("https://example.org/article?secret=opaque")));
        check("Direct video file".equals(
                UltVideoLinkRules.sourceCategory("https://cdn.example.org/a.mp4?token=abc")));
        check("Invalid/excluded link".equals(
                UltVideoLinkRules.sourceCategory("http://instagram.com/reel/x")));
        check("Other HTTPS webpage (metadata only)".equals(
                UltVideoLinkRules.sourceCategory("https://evil-instagram.com/reel/x")));
        check(UltVideoLinkRules.isSiteUrl("https://www.instagram.com/reel/Dft9iuNq8VQ/?bqrl=MWY4cWU2Mmd3OGg4bw=="));
        check(UltVideoLinkRules.isSiteUrl("https://m.youtube.com/watch?v=3-B6fmO08UY"));
        check(!UltVideoLinkRules.isSiteUrl("https://evil-youtube.com/watch?v=x"));
        check(!UltVideoLinkRules.isSiteUrl("https://evil.instagram.com.evil.net/reel/x"));
        check(!UltVideoLinkRules.isSiteUrl("https://127.0.0.1/test.mp4"));
        check(!UltVideoLinkRules.isSiteUrl("http://youtu.be/3-B6fmO08UY"));
        check(!UltVideoLinkRules.hasVideoExtension("https://example.org/page"));
        check("https://cdn.example.org/video.mp4?a=1&b=2".equals(
                UltVideoLinkRules.htmlVideoUrl(
                        "<html><meta content='https://cdn.example.org/video.mp4?a=1&amp;b=2' property='og:video'></html>")));
        check(UltVideoLinkRules.htmlVideoUrl("<meta property='og:image' content='https://a.org/x.jpg'>") == null);
        check(UltVideoLinkRules.htmlVideoUrl("<meta property='og:video' content='http://site.org/x.mp4'>") == null);
        check(UltVideoLinkRules.isSiteUrl("https://www.reddit.com/r/videos/comments/abc/post/"));
        check(UltVideoLinkRules.isSiteUrl("https://v.redd.it/abc"));
        check(!UltVideoLinkRules.isSiteUrl("https://reddit.com.example.org/video"));
        check("Reddit".equals(UltVideoLinkRules.sourceCategory("https://v.redd.it/abc")));
        check("bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b".equals(UltVideoLinkRules.videoFormat(0)));
        check("bv*[ext=mp4][height<=720]+ba[ext=m4a]/b[ext=mp4][height<=720]/b[height<=720]"
                .equals(UltVideoLinkRules.videoFormat(720)));
        check(UltVideoLinkRules.videoFormat(480).contains("[height<=480]"));
        check(UltVideoLinkRules.videoFormat(1080).contains("[height<=1080]"));
        check(UltVideoLinkRules.videoFormat(-1).equals(UltVideoLinkRules.videoFormat(0)));
        check("https://www.instagram.com/reel/abc/".equals(UltVideoLinkRules.extractionUrl(
                "https://www.instagram.com/reel/abc/?igsh=abc&utm_source=share")));
        check("https://instagram.com/p/abc/?unknown=keep%2Bme".equals(UltVideoLinkRules.extractionUrl(
                "https://instagram.com/p/abc/?igshid=abc&unknown=keep%2Bme")));
        check("https://youtu.be/abc".equals(UltVideoLinkRules.extractionUrl(
                "https://youtu.be/abc?si=keep")));
        check("https://www.tiktok.com/@u/video/123".equals(UltVideoLinkRules.extractionUrl(
                "https://www.tiktok.com/@u/video/123?_t=share")));
        check("https://www.facebook.com/watch/?v=123".equals(UltVideoLinkRules.extractionUrl(
                "https://www.facebook.com/watch/?v=123&mibextid=share")));
        check("https://x.com/u/status/123".equals(UltVideoLinkRules.extractionUrl(
                "https://x.com/u/status/123?s=46&t=share")));
        check("https://www.reddit.com/r/videos/comments/abc/?jsc_token=secret&utm_source=app".equals(
                UltVideoLinkRules.extractionUrl("https://www.reddit.com/r/videos/comments/abc/?jsc_token=secret&utm_source=app")));
        check("https://vimeo.com/123".equals(UltVideoLinkRules.extractionUrl("https://vimeo.com/123?share=copy")));
        check(UltVideoLinkRules.isSiteUrl("https://www.threads.com/@u/post/abc"));
        check("Threads".equals(UltVideoLinkRules.sourceCategory("https://www.threads.com/@u/post/abc")));
        check("https://www.threads.com/@u/post/abc".equals(UltVideoLinkRules.extractionUrl(
                "https://www.threads.com/@u/post/abc?xmt=share")));
        check("https://cdn.example.org/a.mp4?utm_source=app".equals(
                UltVideoLinkRules.extractionUrl("https://cdn.example.org/a.mp4?utm_source=app")));
        check("bestaudio[ext=m4a]/bestaudio/best".equals(UltVideoLinkRules.audioFormat()));
        System.out.println("UltVideoLinkRulesTest OK");
    }
}
