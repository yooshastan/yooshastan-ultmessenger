import com.yooshastan.ultmessenger.settings.UltLinkSanitizerPolicy;

public class UltLinkSanitizerPolicyTest {
    static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    static void equal(String actual, String expected) {
        check(expected.equals(actual), "Expected " + expected + " but got " + actual);
    }
    public static void main(String[] args) {
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://www.instagram.com/reel/Dft9iuNq8VQ/?bqrl=MWY4cWU2Mmd3OGg4bw=="),
                "https://www.instagram.com/reel/Dft9iuNq8VQ/");
        // Device regression: anonymous-posted Instagram Reel still had avpt.
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://www.instagram.com/reel/DelvqtyyXy9/?avpt=aWhiYWY3ZmFwbTJ0"),
                "https://www.instagram.com/reel/DelvqtyyXy9/");
        equal(UltLinkSanitizerPolicy.cleanText(
                "watch https://www.instagram.com/reel/DelvqtyyXy9/?avpt=aWhiYWY3ZmFwbTJ0"),
                "watch https://www.instagram.com/reel/DelvqtyyXy9/");
        for (String key : new String[]{"avpt","stkn","cnvx","dmpr","evlk","fqnt",
                                      "gwpx","hvrq","jplx","knvt","lqvp","srtk","xtok"}) {
            equal(UltLinkSanitizerPolicy.cleanUrl(
                    "https://www.instagram.com/reel/ABC123/?"+key+"=share"),
                    "https://www.instagram.com/reel/ABC123/");
        }
        // Functional query fields, signed URLs and other domains are preserved.
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://www.instagram.com/p/ABC123/?avpt=share&img_index=2&hl=en"),
                "https://www.instagram.com/p/ABC123/?img_index=2&hl=en");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://www.instagram.com/reel/ABC123/?avpt=share&token=signed"),
                "https://www.instagram.com/reel/ABC123/?avpt=share&token=signed");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://example.org/reel/ABC123/?avpt=share"),
                "https://example.org/reel/ABC123/?avpt=share");
        equal(UltLinkSanitizerPolicy.cleanUrl("https://youtu.be/3-B6fmO08UY?si=track"),
                "https://youtu.be/3-B6fmO08UY");
        equal(UltLinkSanitizerPolicy.cleanText(
                "watch (https://youtu.be/3-B6fmO08UY?si=123)."),
                "watch (https://youtu.be/3-B6fmO08UY).");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://www.youtube.com/watch?v=abc&t=32s&si=track&utm_source=spam"),
                "https://www.youtube.com/watch?v=abc&t=32s");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://shop.example.org/p?color=blue&gclid=abc&size=m#details"),
                "https://shop.example.org/p?color=blue&size=m#details");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://cdn.example.org/a.mp4?utm_campaign=blah&X-Amz-Signature=secret"),
                "https://cdn.example.org/a.mp4?utm_campaign=blah&X-Amz-Signature=secret");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://example.org/callback?state=ok&gclid=x"),
                "https://example.org/callback?state=ok&gclid=x");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://example.org/verify?code=abc&utm_medium=email"),
                "https://example.org/verify?code=abc&utm_medium=email");
        equal(UltLinkSanitizerPolicy.cleanUrl(
                "https://example.org/search?q=android&utm_medium=app"),
                "https://example.org/search?q=android");
        equal(UltLinkSanitizerPolicy.cleanUrl("http://example.org/a?utm_source=x"),
                "http://example.org/a?utm_source=x");
        equal(UltLinkSanitizerPolicy.cleanUrl("https://127.0.0.1/a?gclid=x"),
                "https://127.0.0.1/a?gclid=x");
        equal(UltLinkSanitizerPolicy.cleanUrl("https://evil.youtube.com.evil.org/a?si=x"),
                "https://evil.youtube.com.evil.org/a?si=x");
        equal(UltLinkSanitizerPolicy.cleanText(
                "Link https://instagram.com/reel/id/?bqrl=abc and https://youtu.be/id?si=x!"),
                "Link https://instagram.com/reel/id/ and https://youtu.be/id!");
        for (String host : new String[]{"x.com", "twitter.com", "mobile.twitter.com", "www.x.com"}) {
            String post = "https://" + host + "/example_user/status/1234567890";
            equal(UltLinkSanitizerPolicy.cleanUrl(post + "?s=46&t=share"), post);
            equal(UltLinkSanitizerPolicy.cleanUrl(post + "?t=share&lang=en&s=20"), post + "?lang=en");
            equal(UltLinkSanitizerPolicy.cleanUrl(post + "?s=46&token=secret&t=share"),
                    post + "?s=46&token=secret&t=share");
        }
        equal(UltLinkSanitizerPolicy.cleanUrl("https://x.com/i/web/status/123?s=20&t=share"),
                "https://x.com/i/web/status/123");
        equal(UltLinkSanitizerPolicy.cleanUrl("https://x.com/search?s=foo&t=bar"),
                "https://x.com/search?s=foo&t=bar");
        equal(UltLinkSanitizerPolicy.cleanUrl("https://evil.x.com.example.org/u/status/123?s=46&t=x"),
                "https://evil.x.com.example.org/u/status/123?s=46&t=x");
        equal(UltLinkSanitizerPolicy.cleanText("watch (https://x.com/u/status/123?s=46&t=share)."),
                "watch (https://x.com/u/status/123).");
        System.out.println("UltLinkSanitizerPolicyTest OK");
    }
}
