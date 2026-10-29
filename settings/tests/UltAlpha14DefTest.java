import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import com.yooshastan.ultmessenger.settings.UltFileInspection;
import com.yooshastan.ultmessenger.settings.UltChatAppearancePolicy;
import com.yooshastan.ultmessenger.settings.UltIdentityCover;
import com.yooshastan.ultmessenger.settings.UltNetworkEvidence;

public final class UltAlpha14DefTest {
    static void ok(boolean check, String why) {
        if (!check) throw new AssertionError(why);
    }
    public static void main(String[] args) throws Exception {
        UltFileInspection.Result original=UltFileInspection.digest(
                new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
        UltFileInspection.Result same=UltFileInspection.digest(
                new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
        UltFileInspection.Result changed=UltFileInspection.digest(
                new ByteArrayInputStream("abd".getBytes(StandardCharsets.UTF_8)));
        ok(UltFileInspection.sameContent(original,same), "identical copy");
        ok(!UltFileInspection.sameContent(original,changed), "different hash");
        ok(!UltFileInspection.sameContent(original,null), "missing file");
        ok(UltChatAppearancePolicy.font(4)==12 && UltChatAppearancePolicy.font(45)==30,
                "font bounds");
        ok(UltChatAppearancePolicy.radius(-2)==0 && UltChatAppearancePolicy.radius(99)==17,
                "bubble bounds");
        ok(UltIdentityCover.summary("someone","123",true).indexOf("someone") < 0,
                "masked identity never exposed");
        ok(UltIdentityCover.summary("someone","123",false).contains("@someone"),
                "explicit local reveal");
        ok(!UltIdentityCover.key(0).equals(UltIdentityCover.key(1)), "account separation");
        boolean[] marked=new boolean[UltNetworkEvidence.PATHS.length];
        marked[0]=true;
        String report=UltNetworkEvidence.render(marked);
        ok(report.contains("NOT ROUTE-VERIFIED") && report.contains("user marked"),
                "no false leak claims");
        ok(!report.contains("someone"), "no identity in capture report");
        System.out.println("ULT unified Alpha 0.14d+ model checks passed");
    }
}
