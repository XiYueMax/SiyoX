package epic.verify.api;




/**
 * AppKey 转换工具：明文 AppKey → 加密 AppKey（Base64，约 344 字符）。
 *
 * 命令行用法（在装有 JRE/JDK 的机器上）：
 *   java -cp epic-verify-sdk.jar epic.verify.api.AppKeyConverter EE862FA143AE0AA9C8ED1D1519A474E6
 *
 * 输出即为"加密 AppKey"，可直接填入 SDK 或注入 dex。
 */
public class AppKeyConverter {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("用法: java -cp epic-verify-sdk.jar epic.verify.api.AppKeyConverter <明文AppKey>");
            System.out.println("明文 AppKey 为 32 位字母数字，在后台「我的应用」获取。");
            return;
        }
        String plain = args[0].trim();
        if (!plain.matches("^[a-zA-Z0-9]{32}$")) {
            System.out.println("[错误] 明文 AppKey 必须为 32 位字母数字，当前值: " + plain);
            return;
        }
        try {
            String enc = convert(plain);
            System.out.println("明文 AppKey: " + plain);
            System.out.println("加密 AppKey: " + enc);
            System.out.println("(长度 " + enc.length() + "，可直接填入 SDK 或注入 dex)");
        } catch (EpicVerifyException e) {
            System.out.println("[错误] 转换失败: " + e.getMessage());
        }
    }

    /** 明文 AppKey -> 加密 AppKey（使用内嵌公钥，PKCS1 随机填充，每次结果不同但均可使用）。 */
    public static String convert(String plaintext) throws EpicVerifyException {
        return EpicVerifySDK.encryptAppKey(plaintext);
    }
}
