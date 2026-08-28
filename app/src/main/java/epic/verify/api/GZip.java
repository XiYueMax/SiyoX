package epic.verify.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZip 压缩/解压工具。
 */
public final class GZip {

    private GZip() {
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.finish();
        gzip.close();
        return bos.toByteArray();
    }

    public static byte[] uncompress(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2);
        byte[] buf = new byte[8192];
        int n;
        while ((n = gzip.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        gzip.close();
        return bos.toByteArray();
    }
}
