package epic.verify.api;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

/**
 * RSA 工具：RSA/ECB/PKCS1Padding，2048 位。
 * - encryptWithPublicKey ：公钥加密（官网换 Key / 请求前加密 AppKey）
 * - decryptWithPublicKey ：公钥“解密”（还原服务器用私钥加密的响应）
 */
public final class RSAUtil {

    public static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;
    private static final int MAX_ENCRYPT_BLOCK = KEY_SIZE / 8 - 11;
    private static final int MAX_DECRYPT_BLOCK = KEY_SIZE / 8;

    private RSAUtil() {
    }

    public static PublicKey readPublicKey(String base64) throws Exception {
        byte[] der = Base64.decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static String encryptWithPublicKey(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] data = plainText.getBytes("UTF-8");
        List<byte[]> parts = new ArrayList<byte[]>();
        int offset = 0;
        while (data.length - offset > 0) {
            int len = Math.min(MAX_ENCRYPT_BLOCK, data.length - offset);
            parts.add(cipher.doFinal(data, offset, len));
            offset += len;
        }
        return Base64.encode(join(parts));
    }

    public static String decryptWithPublicKey(String encryptedText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] encryptedData = Base64.decode(encryptedText);
        List<byte[]> parts = new ArrayList<byte[]>();
        int offset = 0;
        while (encryptedData.length - offset > 0) {
            int len = Math.min(MAX_DECRYPT_BLOCK, encryptedData.length - offset);
            parts.add(cipher.doFinal(encryptedData, offset, len));
            offset += len;
        }
        byte[] plain = join(parts);
        return new String(plain, "UTF-8");
    }

    private static byte[] join(List<byte[]> parts) {
        int total = 0;
        for (byte[] b : parts) total += b.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] b : parts) {
            System.arraycopy(b, 0, out, pos, b.length);
            pos += b.length;
        }
        return out;
    }
}
