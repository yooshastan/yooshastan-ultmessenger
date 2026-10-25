import com.yooshastan.ultmessenger.dns.UltDoqBootstrap;
import java.util.Arrays;

public final class UltDoqBootstrapTest {
    private static void reject(String input) {
        try {
            UltDoqBootstrap.ipv4(input);
            throw new AssertionError("Accepted unsafe IP: " + input);
        } catch (IllegalArgumentException expected) {
            // Pass: no platform hostname resolution.
        }
    }

    public static void main(String[] args) {
        if (!Arrays.equals(UltDoqBootstrap.ipv4("94.140.14.14"),
                new byte[] {94, (byte) 140, 14, 14})) {
            throw new AssertionError("Incorrect numeric bootstrap address");
        }
        for (String s : new String[] {"dns.example.org", "127.0.0", "8.8.08.8",
                "8.8.8.256", "0.0.0.0", "224.0.0.1", "8.8.8.8 ",
                "8.8.8.-1", "[::1]"}) {
            reject(s);
        }
        System.out.println("UltDoqBootstrapTest PASS");
    }
}
