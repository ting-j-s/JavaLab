import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class TestServerIntegration {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;

        // === Test 1: correct login ===
        System.out.println("=== Test 1: correct login ===");
        Socket s1 = new Socket(host, port);
        PrintWriter w1 = new PrintWriter(new OutputStreamWriter(s1.getOutputStream(), "UTF-8"), true);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        send(w1, "LOGIN|zhangsan||123456|0");
        assertContains("correct login", r1.readLine(), "LOGIN_SUCCESS");

        // === Test 2: wrong password ===
        System.out.println("\n=== Test 2: wrong password ===");
        Socket s2 = new Socket(host, port);
        PrintWriter w2 = new PrintWriter(new OutputStreamWriter(s2.getOutputStream(), "UTF-8"), true);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));
        send(w2, "LOGIN|zhangsan||wrongpass|0");
        assertContains("wrong password", r2.readLine(), "LOGIN_FAIL");
        s2.close();

        // === Test 3: non-existent user ===
        System.out.println("\n=== Test 3: non-existent user ===");
        Socket s3 = new Socket(host, port);
        PrintWriter w3 = new PrintWriter(new OutputStreamWriter(s3.getOutputStream(), "UTF-8"), true);
        BufferedReader r3 = new BufferedReader(new InputStreamReader(s3.getInputStream(), "UTF-8"));
        send(w3, "LOGIN|ghost||123456|0");
        assertContains("non-existent user", r3.readLine(), "LOGIN_FAIL");
        s3.close();

        // === Test 4: duplicate login ===
        System.out.println("\n=== Test 4: duplicate login ===");
        Socket s4 = new Socket(host, port);
        PrintWriter w4 = new PrintWriter(new OutputStreamWriter(s4.getOutputStream(), "UTF-8"), true);
        BufferedReader r4 = new BufferedReader(new InputStreamReader(s4.getInputStream(), "UTF-8"));
        send(w4, "LOGIN|zhangsan||123456|0");
        assertContains("duplicate login", r4.readLine(), "LOGIN_FAIL");
        s4.close();

        // === Test 5: broadcast ===
        System.out.println("\n=== Test 5: broadcast ===");
        Socket s5 = new Socket(host, port);
        PrintWriter w5 = new PrintWriter(new OutputStreamWriter(s5.getOutputStream(), "UTF-8"), true);
        BufferedReader r5 = new BufferedReader(new InputStreamReader(s5.getInputStream(), "UTF-8"));
        send(w5, "LOGIN|lisi||123456|0");
        assertContains("lisi login", r5.readLine(), "LOGIN_SUCCESS");

        // Consume USER_JOIN that zhang receives
        String userJoin = r1.readLine();
        assertContains("zhang sees lisi join", userJoin, "USER_JOIN");
        assertContains("join mentions lisi", userJoin, "lisi");

        // Now zhang broadcasts — broadcast() includes sender, so zhang also gets it
        send(w1, "BROADCAST|zhangsan|ALL|hello everyone|0");
        // lisi sees the broadcast
        assertContains("lisi sees broadcast", r5.readLine(), "hello everyone");
        // zhang also gets their own broadcast
        assertContains("zhang sees own broadcast", r1.readLine(), "hello everyone");

        // === Test 6: private chat ===
        System.out.println("\n=== Test 6: private chat ===");
        send(w5, "PRIVATE|lisi|zhangsan|secret message|0");
        // lisi gets confirmation
        String confirm = r5.readLine();
        assertContains("lisi gets private confirm", confirm, "private message sent");
        // zhang gets the private message
        String priv = r1.readLine();
        assertContains("zhang sees private content", priv, "secret message");
        assertContains("zhang sees PRIVATE type", priv, "PRIVATE");

        // === Test 7: system list ===
        System.out.println("\n=== Test 7: system list ===");
        send(w1, "SYSTEM|zhangsan||list|0");
        String listResp = r1.readLine();
        assertContains("list contains zhangsan", listResp, "zhangsan");
        assertContains("list contains lisi", listResp, "lisi");

        // === Test 8: anonymous mode ===
        System.out.println("\n=== Test 8: anonymous mode ===");
        send(w1, "SYSTEM|zhangsan||showanonymous|0");
        assertContains("showanonymous real name", r1.readLine(), "real name");

        send(w1, "SYSTEM|zhangsan||anonymous|0");
        assertContains("switch to anonymous", r1.readLine(), "switched to anonymous");

        // Broadcast while anonymous
        send(w1, "BROADCAST|zhangsan|ALL|im anonymous|0");
        // lisi sees it from "anonymous"
        String anonForLisi = r5.readLine();
        assertContains("lisi sees anonymous from", anonForLisi, "anonymous");
        assertContains("lisi sees anonymous content", anonForLisi, "im anonymous");
        // zhang also gets their own broadcast
        String anonForSelf = r1.readLine();
        assertContains("zhang sees own anon broadcast", anonForSelf, "im anonymous");

        send(w1, "SYSTEM|zhangsan||showanonymous|0");
        assertContains("showanonymous after", r1.readLine(), "anonymous");

        // Switch back to real name
        send(w1, "SYSTEM|zhangsan||anonymous|0");
        assertContains("switch back to real", r1.readLine(), "switched to real name");

        // === Test 9: quit and USER_LEAVE ===
        System.out.println("\n=== Test 9: quit and USER_LEAVE ===");
        send(w5, "SYSTEM|lisi||quit|0");
        assertContains("quit response", r5.readLine(), "bye");

        // zhang should see USER_LEAVE for lisi
        String leaveMsg = r1.readLine();
        assertContains("USER_LEAVE type", leaveMsg, "USER_LEAVE");
        assertContains("leave mentions lisi", leaveMsg, "lisi");

        s5.close();

        // === Test 10: retry login ===
        System.out.println("\n=== Test 10: retry login ===");
        Socket s10 = new Socket(host, port);
        PrintWriter w10 = new PrintWriter(new OutputStreamWriter(s10.getOutputStream(), "UTF-8"), true);
        BufferedReader r10 = new BufferedReader(new InputStreamReader(s10.getInputStream(), "UTF-8"));
        send(w10, "LOGIN|wangwu||badpass|0");
        assertContains("first fail", r10.readLine(), "LOGIN_FAIL");
        send(w10, "LOGIN|wangwu||123456|0");
        assertContains("second success", r10.readLine(), "LOGIN_SUCCESS");

        // zhang sees wangwu join
        String wangwuJoin = r1.readLine();
        assertContains("zhang sees wangwu join", wangwuJoin, "wangwu");

        send(w10, "SYSTEM|wangwu||quit|0");
        r10.readLine(); // bye
        s10.close();

        // zhang sees wangwu leave
        r1.readLine(); // USER_LEAVE for wangwu

        // Cleanup: quit zhang
        send(w1, "SYSTEM|zhangsan||quit|0");
        r1.readLine(); // bye
        s1.close();

        System.out.println("\n========== " + passed + " passed, " + failed + " failed ==========");
        if (failed > 0) System.exit(1);
    }

    static void send(PrintWriter w, String msg) {
        System.out.println("  >> " + msg.substring(0, Math.min(60, msg.length())));
        w.println(msg);
        w.flush();
    }

    static String readLine(BufferedReader r) throws Exception {
        return r.readLine();
    }

    static void assertContains(String label, String actual, String expected) {
        if (actual != null && actual.contains(expected)) {
            System.out.println("  OK: " + label);
            passed++;
        } else {
            System.out.println("  FAIL: " + label + " — expected '" + expected + "', got: " + actual);
            failed++;
        }
    }
}
