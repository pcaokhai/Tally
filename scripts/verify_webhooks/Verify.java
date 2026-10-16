// Verifies Tally-Signature per docs/03 §4 against every golden vector.
// Single-file, no build: java Verify.java [path/to/signature-vectors.json]
// Uses only the JDK (java.net.http... not needed) plus a hand-rolled minimal JSON reader,
// since this repo's JSON libs aren't guaranteed on a receiver's bare `java Verify.java` run.
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Verify {
    static String sign(String secret, long t, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal((t + "." + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    static String classify(String header, String rawBody, List<String> secrets, long now, long toleranceSeconds) throws Exception {
        String[] parts = header.split(",");
        long t = Long.parseLong(parts[0].substring(2));
        List<String> sigs = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) sigs.add(parts[i].substring(3));
        if (now - t > toleranceSeconds) return "REJECT_TIMESTAMP";
        for (String secret : secrets) {
            String expected = sign(secret, t, rawBody);
            for (String sig : sigs) if (constantTimeEquals(expected, sig)) return "VALID";
        }
        return "REJECT_SIGNATURE";
    }

    // Minimal extraction, deliberately not a general JSON parser (ponytail: this repo's
    // vectors file has a fixed, known shape; a real client should use its normal JSON lib).
    // raw_body is itself JSON, so we split top-level vector objects by brace depth rather
    // than a brace-free regex.
    static List<String> splitObjects(String arrayBody) {
        List<String> objs = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) objs.add(arrayBody.substring(start, i + 1)); }
        }
        return objs;
    }

    static List<Vector> parse(String json) {
        List<Vector> vectors = new ArrayList<>();
        int arrStart = json.indexOf("\"vectors\"");
        arrStart = json.indexOf('[', arrStart);
        int arrEnd = json.lastIndexOf(']');
        long tolerance = Long.parseLong(field(json, "tolerance_seconds"));
        for (String b : splitObjects(json.substring(arrStart, arrEnd + 1))) {
            Vector v = new Vector();
            v.name = field(b, "name");
            v.header = field(b, "header");
            v.rawBody = field(b, "raw_body");
            v.now = Long.parseLong(field(b, "now"));
            v.expected = field(b, "expected");
            v.tolerance = tolerance;
            v.secrets = new ArrayList<>();
            Matcher sm = Pattern.compile("\"secrets_valid_for_receiver\"\\s*:\\s*\\[([^]]*)]").matcher(b);
            if (sm.find()) {
                for (String s : sm.group(1).split(",")) {
                    String t = s.trim().replaceAll("^\"|\"$", "");
                    if (!t.isEmpty()) v.secrets.add(t);
                }
            }
            vectors.add(v);
        }
        return vectors;
    }

    static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?((?:[^\"\\\\]|\\\\.)*)\"?[,}]").matcher(json);
        if (!m.find()) throw new IllegalStateException("missing field " + key);
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    static class Vector {
        String name, header, rawBody, expected;
        long now, tolerance;
        List<String> secrets;
    }

    public static void main(String[] args) throws Exception {
        // Default assumes `java Verify.java` run from scripts/verify_webhooks/ (repo layout).
        Path file = args.length > 0 ? Path.of(args[0]) : Path.of("../../contracts/webhooks/signature-vectors.json");
        String json = Files.readString(file);
        List<Vector> vectors = parse(json);
        for (Vector v : vectors) {
            String got = classify(v.header, v.rawBody, v.secrets, v.now, v.tolerance);
            if (!got.equals(v.expected)) {
                System.err.println("FAIL " + v.name + ": got " + got + ", expected " + v.expected);
                System.exit(1);
            }
            System.out.println("ok " + v.name + " -> " + got);
        }
        System.out.println(vectors.size() + " vectors verified (java)");
    }
}
