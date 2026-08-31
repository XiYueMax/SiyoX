package epic.verify.api;

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

public static String convert(String plaintext) throws EpicVerifyException {
        return EpicVerifySDK.encryptAppKey(plaintext);
    }
}
