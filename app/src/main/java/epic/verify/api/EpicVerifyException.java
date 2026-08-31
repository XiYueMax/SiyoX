package epic.verify.api;

public class EpicVerifyException extends Exception {

    public EpicVerifyException(String message) {
        super(message);
    }

    public EpicVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
