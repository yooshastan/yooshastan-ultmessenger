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
        check(!UltVideoLinkRules.hasVideoExtension("https://example.org/page"));
        check("https://cdn.example.org/video.mp4?a=1&b=2".equals(
                UltVideoLinkRules.htmlVideoUrl(
                        "<html><meta content='https://cdn.example.org/video.mp4?a=1&amp;b=2' property='og:video'></html>")));
        check(UltVideoLinkRules.htmlVideoUrl("<meta property='og:image' content='https://a.org/x.jpg'>") == null);
        check(UltVideoLinkRules.htmlVideoUrl("<meta property='og:video' content='http://site.org/x.mp4'>") == null);
        System.out.println("UltVideoLinkRulesTest OK");
    }
}
