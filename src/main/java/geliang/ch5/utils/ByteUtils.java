package geliang.ch5.utils;

public class ByteUtils {
    public static boolean startWith(byte[] bytes, byte[] head) {
        if (bytes.length < head.length) {
            return false;
        }
        for (int i = 0; i < head.length; i++) {
            if (head[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
