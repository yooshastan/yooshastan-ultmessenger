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
        System.out.println("UltVideoLinkRulesTest OK");
    }
}
