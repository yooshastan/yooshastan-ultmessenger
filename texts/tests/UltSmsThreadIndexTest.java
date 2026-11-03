import com.yooshastan.ultmessenger.texts.UltSmsThreadIndex;
import java.util.Arrays;
import java.util.List;

public final class UltSmsThreadIndexTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        List<UltSmsThreadIndex.Message> rows = Arrays.asList(
                new UltSmsThreadIndex.Message(4, 1, 100, "A", "old", false,
                        UltSmsThreadIndex.Kind.SMS),
                new UltSmsThreadIndex.Message(4, 2, 400, "A", "latest", true,
                        UltSmsThreadIndex.Kind.MMS),
                new UltSmsThreadIndex.Message(5, 3, 200, "B", "second", true,
                        UltSmsThreadIndex.Kind.SMS));
        List<UltSmsThreadIndex.Thread> threads = UltSmsThreadIndex.group(rows);
        check(threads.size() == 2, "one display row per native SMS thread");
        check(threads.get(0).threadId == 4, "most recent first");
        check(threads.get(0).mostRecentMessageId == 2, "latest message kept");
        check(threads.get(0).unreadMessages == 1, "unread aggregated");
        check(threads.get(0).lastKind == UltSmsThreadIndex.Kind.MMS, "MMS recognized");
        check(threads.get(1).threadId == 5, "second thread");
        check(UltSmsThreadIndex.group(null).isEmpty(), "empty provider");
        boolean invalidRejected = false;
        try {
            new UltSmsThreadIndex.Message(0, 99, 1, "A", "bad", true,
                    UltSmsThreadIndex.Kind.SMS);
        } catch (IllegalArgumentException expected) {
            invalidRejected = true;
        }
        check(invalidRejected, "invalid provider rows rejected");
        System.out.println("UltSmsThreadIndexTest OK");
    }
}
