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
        System.out.println("UltLinkSanitizerPolicyTest OK");
    }
}
