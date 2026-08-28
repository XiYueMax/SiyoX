package epic.verify.api;

/**
 * SDK 异常：网络错误 / 协议错误 / 加解密失败等。
 */
public class EpicVerifyException extends Exception {

    public EpicVerifyException(String message) {
        super(message);
    }

    public EpicVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
