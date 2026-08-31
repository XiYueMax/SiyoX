package epic.verify.api;

public final class Base64 {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static final int[] DECODE = new int[128];

    static {
        for (int i = 0; i < 128; i++) DECODE[i] = -1;
        for (int i = 0; i < ALPHABET.length; i++) DECODE[ALPHABET[i]] = i;
    }

    private Base64() {
    }

    public static String encode(byte[] data) {
        if (data == null) return null;
        StringBuilder sb = new StringBuilder(((data.length + 2) / 3) * 4);
        int i = 0;
        while (i + 2 < data.length) {
            int v = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8) | (data[i + 2] & 0xff);
            sb.append(ALPHABET[(v >>> 18) & 0x3f]);
            sb.append(ALPHABET[(v >>> 12) & 0x3f]);
            sb.append(ALPHABET[(v >>> 6) & 0x3f]);
            sb.append(ALPHABET[v & 0x3f]);
            i += 3;
        }
        int rem = data.length - i;
        if (rem == 1) {
            int v = (data[i] & 0xff) << 16;
            sb.append(ALPHABET[(v >>> 18) & 0x3f]);
            sb.append(ALPHABET[(v >>> 12) & 0x3f]);
            sb.append('=');
            sb.append('=');
        } else if (rem == 2) {
            int v = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8);
            sb.append(ALPHABET[(v >>> 18) & 0x3f]);
            sb.append(ALPHABET[(v >>> 12) & 0x3f]);
            sb.append(ALPHABET[(v >>> 6) & 0x3f]);
            sb.append('=');
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        if (s == null) return null;
        int len = s.length();
        int pad = 0;
        if (len > 0 && s.charAt(len - 1) == '=') pad++;
        if (len > 1 && s.charAt(len - 2) == '=') pad++;
        byte[] out = new byte[(len / 4) * 3 - pad];
        int outPos = 0;
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '=') break;
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') continue;
            int d = c < 128 ? DECODE[c] : -1;
            if (d < 0) throw new IllegalArgumentException("invalid base64 char: " + c);
            acc = (acc << 6) | d;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                if (outPos < out.length) out[outPos++] = (byte) ((acc >> bits) & 0xff);
            }
        }
        if (outPos != out.length) {
            byte[] trimmed = new byte[outPos];
            System.arraycopy(out, 0, trimmed, 0, outPos);
            return trimmed;
        }
        return out;
    }
}
