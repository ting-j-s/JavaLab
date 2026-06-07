import common.Message;
import common.MessageType;

public class TestMessage {

    public static void main(String[] args) {
        System.out.println("=== Test 1: basic encode / decode ===");
        Message m1 = new Message(MessageType.LOGIN, "zhangsan", null, "123456");
        String encoded = m1.encode();
        System.out.println("encoded: " + encoded);

        Message decoded = Message.decode(encoded);
        System.out.println("decoded: " + decoded);

        assertEq(m1.getType(), decoded.getType(), "type");
        assertEq(m1.getFrom(), decoded.getFrom(), "from");
        assertEq(m1.getTo(), decoded.getTo(), "to");
        assertEq(m1.getContent(), decoded.getContent(), "content");
        assertEq(m1.getTimestamp(), decoded.getTimestamp(), "timestamp");

        System.out.println();

        System.out.println("=== Test 2: content contains | ===");
        Message m2 = new Message(MessageType.BROADCAST, "lisi", "ALL", "hello | world | test");
        String e2 = m2.encode();
        System.out.println("encoded: " + e2);
        Message d2 = Message.decode(e2);
        System.out.println("decoded: " + d2);
        assertEq("hello | world | test", d2.getContent(), "content with pipes");

        System.out.println();

        System.out.println("=== Test 3: null fields ===");
        Message m3 = new Message(MessageType.SYSTEM, null, null, null);
        String e3 = m3.encode();
        System.out.println("encoded: " + e3);
        Message d3 = Message.decode(e3);
        System.out.println("decoded: " + d3);
        assertEq(null, d3.getFrom(), "null from");
        assertEq(null, d3.getTo(), "null to");

        System.out.println();

        System.out.println("=== Test 4: all message types ===");
        for (MessageType mt : MessageType.values()) {
            Message m = new Message(mt, "user", "target", "test content");
            String e = m.encode();
            Message d = Message.decode(e);
            assertEq(mt, d.getType(), "round-trip " + mt);
        }
        System.out.println("All message types OK");

        System.out.println();

        System.out.println("=== Test 5: decode invalid line ===");
        try {
            Message.decode("bad line");
            System.out.println("FAIL: expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("OK: caught " + e.getMessage());
        }

        try {
            Message.decode(null);
            System.out.println("FAIL: expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("OK: caught " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== All tests passed ===");
    }

    private static void assertEq(Object expected, Object actual, String label) {
        boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
        if (!ok) {
            System.out.println("FAIL [" + label + "]: expected=" + expected + ", actual=" + actual);
            System.exit(1);
        } else {
            System.out.println("  OK [" + label + "]");
        }
    }
}
