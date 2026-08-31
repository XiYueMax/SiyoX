package epic.verify.api;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public final class Proto {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream(256);

    public void writeTag(int field, int wireType) {
        writeVarint(((long) field << 3) | wireType);
    }

    public void writeVarint(long v) {
        while ((v & ~0x7fL) != 0L) {
            out.write((int) ((v & 0x7fL) | 0x80L));
            v >>>= 7;
        }
        out.write((int) v);
    }

    public void writeInt32Field(int field, int value) {
        writeTag(field, 0);
        writeVarint(value & 0xffffffffL);
    }

    public void writeInt64Field(int field, long value) {
        writeTag(field, 0);
        writeVarint(value);
    }

    public void writeBytesField(int field, byte[] value) {
        writeTag(field, 2);
        writeVarint(value.length);
        out.write(value, 0, value.length);
    }

    public void writeStringField(int field, String value) {
        try {
            writeBytesField(field, value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            writeBytesField(field, value.getBytes());
        }
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
