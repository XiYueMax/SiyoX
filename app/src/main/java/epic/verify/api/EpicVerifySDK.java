package epic.verify.api;

import epic.verify.api.Json.Obj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;










/**
 * EpicVerify SDK —— 与后端验证服务（TCP 5000）逐字节兼容的纯 Java 客户端。
 *
 * 用法：
 *   EpicVerifySDK sdk = new EpicVerifySDK("epic.z74d.top", 5000, "官网换取的加密AppKey");
 *   sdk.setDeviceId(deviceId);
 *   sdk.setCard(card);
 *   Resp r = sdk.cardVerify();          // code==200 即成功
 *
 * 全功能：卡密验证 / 在线心跳 / 卡密查询 / 解绑 / 密码验证 / 云端配置 / 公告 / 版本升级。
 * JDK7 兼容，零第三方依赖。
 */
public class EpicVerifySDK {

    public static final String SDK_VERSION = "1.0.0";

    /** 服务器 RSA 公钥（DER Base64），与服务器 verify_key/public_key.pem 配套。 */
    public static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiGcQ9YZC70OTUFdZtTfsqXgTwwK90bwU0p5sE9yi/kfebKs2JAi8JSeBSKJkaZ3T4+Q9oCWTnLrpkgqMCkNq/3GY2aTpyp/c4Tgl2ckf1Chfoy27fLcDJWjK/zBUzgLTjV0L0eOG7L7Gr+cJgcYrUPv7CXch773JKcNK0ce/uZD2+GChM+zOycaVbeJj1+WYGo0Dq8aTYqiJh99tKEKyGT7O3JKbyEb2jyeQO/TCCsAoVOulQcuGXUGageBrJnnod3J0QUdF30SMOLtlKk4tDjb+ptv7rk58EIYFcyzwAufiucQ8Vj0FgGm92/fZ9r29dnTDI+VTeZ7JGo+kiBIAkwIDAQAB";

    private static final String MAGIC = "Epic";
    private static final int TYPE_VERIFY = 1;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private static final int MAX_PAYLOAD = 5 * 1024 * 1024;

    private final String[] hosts;
    private final int port;
    private final String appKey;
    private final PublicKey publicKey;
    private volatile String lastServer;

    private boolean debug = false;
    private String deviceId = "";
    private String locale = "zh";
    private String statusMachine = "";
    private int appVersion = 0;
    private String packageName = "";
    private String card = "";
    private boolean autoHeartbeat = true;
    private long heartbeatIntervalMs = 5 * 60 * 1000L;
    private int retryCount = 3;

    private HeartbeatFailListener heartbeatFailListener;
    private HeartbeatListener heartbeatListener;
    private Timer heartbeatTimer;

    private volatile String token;
    private volatile long expire = -1L;
    private volatile Resp lastHeartbeat;
    private volatile Resp lastSoftConfig;

    public interface HeartbeatFailListener {
        void onHeartbeatFail(Resp resp);
    }

    /** 通用心跳监听：每次心跳（成功/失败）都会回调，便于实时展示。 */
    public interface HeartbeatListener {
        void onHeartbeat(Resp resp);
    }

    /** 登录结果（Card.Verify / Card.Pass / Card.Valid 成功后有效）。 */
    public static class LoginResult {
        public String token;
        public long expire;

        public boolean isValid() {
            return token != null && token.length() > 0;
        }
    }

    /** 按钮：文本/事件/值。文本为空或未配置则该按钮不启用（后端控制）。 */
    public static class Btn {
        public String text;   // {prefix}_btn_value
        public int event;     // {prefix}_btn_event（0退出 1网页 2QQ 3QQ群 4查卡 5解绑 6开应用 7分享 8分享QQ 9分享微信 10复制）
        public String value;  // {prefix}_btn_event_value（无则回退 {prefix}_event_value）

        public boolean enabled() {
            return text != null && text.length() > 0;
        }
    }

    /** 公告（来源 Soft.announcementInfo，字段见 epic_soft_module_info）。 */
    public static class Notice {
        public String title;          // title_value
        public String content;        // msg_value
        public Btn confirm = new Btn(); // confirm_btn_*
        public Btn cancel = new Btn();  // cancel_btn_*
        public Btn extra = new Btn();   // extra_btn_*
        public boolean diffShowEnable;// diff_show_enable
        public int announcementMode;  // announcement_mode
        public int dialogStyle;       // dialog_style
        public int showType;          // show_type
        public String raw;

        public boolean hasNotice() {
            return raw != null && raw.length() > 0;
        }
    }

    /** 版本（来源 Soft.upgradeInfo，字段见 epic_soft_module_info）。 */
    public static class Version {
        public String title;          // title_value
        public String content;        // msg_value
        public int version;           // upgrade_version
        public String downloadUrl;    // upgrade_url
        public int updateType;        // upgrade_type
        public String upgradeBtn;     // upgrade_btn_value
        public String cancelBtn;      // cancel_btn_value
        public String ignoreBtn;      // ignore_btn_value
        public String updateLog;      // update_log
        public boolean cancelEnable;  // upgrade_cancel_enable
        public boolean ignoreEnable;  // upgrade_ignore_enable
        public int dialogStyle;       // dialog_style
        public int showType;          // show_type
        public boolean hasUpdate;
        public String raw;
    }

    /** 网络验证模块（来源 Soft.verifyInfo，字段见 epic_soft_module_info）。 */
    public static class VerifyConfig {
        public String title;            // title_value
        public String content;          // msg_value
        public int bindingType;         // binding_type
        public int heartbeatType;       // heartbeat_type
        public int heartbeatTime;       // heartbeat_time（分钟）
        public int heartbeatEvent;      // heartbeat_event
        public String heartbeatEventValue; // heartbeat_event_value
        public String cardPlaceholder;  // card_placeholder
        public String secretKey;        // secretKey
        public Btn confirm = new Btn(); // confirm_btn_*
        public Btn cancel = new Btn();  // cancel_btn_*
        public Btn extra = new Btn();   // extra_btn_*
        public int dialogStyle;         // dialog_style
        public int showType;            // show_type
        public String raw;

        public boolean hasVerify() {
            return raw != null && raw.length() > 0;
        }
    }

    /** 密码进入模块（来源 Soft.passInfo，字段见 epic_soft_module_info）。 */
    public static class Pass {
        public int passType;        // pass_type
        public String title;        // title_value
        public String content;      // msg_value
        public Btn confirm = new Btn(); // confirm_btn_*
        public Btn cancel = new Btn();  // cancel_btn_*
        public Btn extra = new Btn();   // extra_btn_*
        public String pass;         // pass（模块内配置的密码）
        public int showType;        // show_type
        public String raw;

        public boolean hasPass() {
            return raw != null && raw.length() > 0;
        }
    }

    public EpicVerifySDK(String host, int port, String appKey) throws EpicVerifyException {
        this(new String[]{host}, port, appKey, PUBLIC_KEY_BASE64);
    }

    /** 多服务器（主/备）构造器：连接失败自动按顺序切换下一个 host。 */
    public EpicVerifySDK(String[] hosts, int port, String appKey) throws EpicVerifyException {
        this(hosts, port, appKey, PUBLIC_KEY_BASE64);
    }

    /** 可指定公钥的构造器（默认使用内嵌公钥，一般仅需使用默认构造器）。 */
    public EpicVerifySDK(String host, int port, String appKey, String publicKeyBase64) throws EpicVerifyException {
        this(new String[]{host}, port, appKey, publicKeyBase64);
    }

    /** 多服务器 + 自定义公钥（一般用前两个构造器即可）。 */
    public EpicVerifySDK(String[] hosts, int port, String appKey, String publicKeyBase64) throws EpicVerifyException {
        if (hosts == null || hosts.length == 0) throw new EpicVerifyException("hosts is empty");
        for (String h : hosts) {
            if (h == null || h.length() == 0) throw new EpicVerifyException("host is empty");
        }
        if (appKey == null || appKey.length() == 0) throw new EpicVerifyException("appKey is empty");
        if (publicKeyBase64 == null || publicKeyBase64.length() == 0) throw new EpicVerifyException("publicKey is empty");
        this.hosts = hosts;
        this.port = port;
        try {
            this.publicKey = RSAUtil.readPublicKey(publicKeyBase64);
        } catch (Exception e) {
            throw new EpicVerifyException("init rsa public key failed: " + e.getMessage(), e);
        }
        // appKey 自适应：344 位 Base64 视为"已是加密 AppKey"；否则视为"明文 AppKey"，自动用公钥加密
        if (isEncryptedAppKey(appKey)) {
            this.appKey = appKey;
        } else {
            try {
                this.appKey = RSAUtil.encryptWithPublicKey(appKey, this.publicKey);
            } catch (Exception e) {
                throw new EpicVerifyException("encrypt appkey failed: " + e.getMessage(), e);
            }
        }
    }

    private static boolean isEncryptedAppKey(String appKey) {
        if (appKey == null || appKey.length() != 344) return false;
        for (int i = 0; i < appKey.length(); i++) {
            char c = appKey.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=')) {
                return false;
            }
        }
        return true;
    }

    // ==================== 配置项 ====================

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId == null ? "" : deviceId;
    }

    public void setLocale(String locale) {
        this.locale = locale == null ? "" : locale;
    }

    public void setStatusMachine(String statusMachine) {
        this.statusMachine = statusMachine == null ? "" : statusMachine;
    }

    public void setAppVersion(int appVersion) {
        this.appVersion = appVersion;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? "" : packageName;
    }

    public void setCard(String card) {
        this.card = card == null ? "" : card;
    }

    public void setAutoHeartbeat(boolean autoHeartbeat) {
        this.autoHeartbeat = autoHeartbeat;
    }

    public void setHeartbeatInterval(long intervalMillis) {
        this.heartbeatIntervalMs = intervalMillis;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setOnHeartbeatFailListener(HeartbeatFailListener listener) {
        this.heartbeatFailListener = listener;
    }

    /** 通用心跳监听：每次心跳都回调（含成功），适合实时打印到日志。 */
    public void setOnHeartbeatListener(HeartbeatListener listener) {
        this.heartbeatListener = listener;
    }

    // ==================== 状态读取 ====================

    public LoginResult getLoginResult() {
        if (token == null || token.length() == 0) return null;
        LoginResult r = new LoginResult();
        r.token = token;
        r.expire = expire;
        return r;
    }

    /** 剩余有效秒数（未验证返回 -1）。 */
    public long getTimeRemaining() {
        if (expire <= 0) return -1L;
        long ms = expire - System.currentTimeMillis();
        return ms <= 0 ? 0L : ms / 1000L;
    }

    public Resp getHeartbeatResult() {
        return lastHeartbeat;
    }

    public Resp getLastSoftConfig() {
        return lastSoftConfig;
    }

    /** 最近一次成功连接的服务器（主备切换时用于排查）。 */
    public String getLastServer() {
        return lastServer;
    }

    // ==================== 工具 ====================

    /** 明文 AppKey -> 加密 AppKey（使用内嵌公钥，PKCS1 随机填充，每次结果不同）。 */
    public static String encryptAppKey(String plaintext) throws EpicVerifyException {
        try {
            return RSAUtil.encryptWithPublicKey(plaintext, RSAUtil.readPublicKey(PUBLIC_KEY_BASE64));
        } catch (Exception e) {
            throw new EpicVerifyException("encrypt appkey failed: " + e.getMessage(), e);
        }
    }

    // ==================== 卡密 API ====================

    /** 卡密验证（Card.Verify）。成功后自动开始心跳。 */
    public Resp cardVerify() throws EpicVerifyException {
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("card", card);
        data.put("mac", deviceId);
        Resp resp = requestCard(0, data);
        if (resp.isSuccess()) handleLoginSuccess(resp);
        return resp;
    }

    /** 在线校验 / 心跳（Card.Valid）。 */
    public Resp cardValid() throws EpicVerifyException {
        if (token == null || token.length() == 0) {
            throw new EpicVerifyException("not logged in, token is empty");
        }
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("card", card);
        data.put("token", token);
        data.put("mac", deviceId);
        Resp resp = requestCard(2, data);
        if (resp.isSuccess()) {
            if (resp.data != null) {
                String t = resp.data.getString("token");
                if (t != null && t.length() > 0) token = t;
                long ex = resp.data.getLong("expire");
                if (ex > 0) expire = ex;
            }
            lastHeartbeat = resp;
        } else {
            lastHeartbeat = resp;
        }
        return resp;
    }

    /** 卡密查询（Card.Query）。 */
    public Resp cardQuery() throws EpicVerifyException {
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("card", card);
        return requestCard(1, data);
    }

    /** 卡密解绑（Card.Unbind）。 */
    public Resp cardUnbind() throws EpicVerifyException {
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("card", card);
        data.put("mac", deviceId);
        return requestCard(3, data);
    }

    /** 密码验证（Card.Pass）。成功后自动开始心跳。 */
    public Resp cardPass() throws EpicVerifyException {
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("card", card);
        Resp resp = requestCard(4, data);
        if (resp.isSuccess()) handleLoginSuccess(resp);
        return resp;
    }

    // ==================== 应用配置 API ====================

    /** 拉取应用云端配置（Soft）。 */
    public Resp getSoftwareConfig() throws EpicVerifyException {
        Resp resp = requestSoft();
        lastSoftConfig = resp;
        return resp;
    }

    /** 远程公告（来源 Soft.announcementInfo）。 */
    public Notice getSoftwareNotice() throws EpicVerifyException {
        return parseNotice(getSoftwareConfig());
    }

    public Notice getSoftwareNotice(Resp softConfig) throws EpicVerifyException {
        if (softConfig == null) return getSoftwareNotice();
        return parseNotice(softConfig);
    }

    /** 应用最新版本（来源 Soft.upgradeInfo）。 */
    public Version getSoftwareLatestVersion(String currentVersion) throws EpicVerifyException {
        return parseVersion(getSoftwareConfig(), currentVersion);
    }

    public Version getSoftwareLatestVersion(String currentVersion, Resp softConfig) throws EpicVerifyException {
        if (softConfig == null) return getSoftwareLatestVersion(currentVersion);
        return parseVersion(softConfig, currentVersion);
    }

    /** 网络验证模块配置（来源 Soft.verifyInfo）。 */
    public VerifyConfig getVerifyConfig() throws EpicVerifyException {
        return parseVerifyConfig(getSoftwareConfig());
    }

    public VerifyConfig getVerifyConfig(Resp softConfig) throws EpicVerifyException {
        if (softConfig == null) return getVerifyConfig();
        return parseVerifyConfig(softConfig);
    }

    /** 密码进入模块配置（来源 Soft.passInfo）。 */
    public Pass getSoftwarePass() throws EpicVerifyException {
        return parsePass(getSoftwareConfig());
    }

    public Pass getSoftwarePass(Resp softConfig) throws EpicVerifyException {
        if (softConfig == null) return getSoftwarePass();
        return parsePass(softConfig);
    }

    // ==================== 生命周期 ====================

    /** 释放资源：停止心跳并关闭内部线程。 */
    public void release() {
        synchronized (this) {
            if (heartbeatTimer != null) {
                heartbeatTimer.cancel();
                heartbeatTimer = null;
            }
        }
    }

    // ==================== 内部实现 ====================

    private void handleLoginSuccess(Resp resp) {
        if (resp.data != null) {
            String t = resp.data.getString("token");
            if (t != null && t.length() > 0) token = t;
            long ex = resp.data.getLong("expire");
            if (ex > 0) expire = ex;
        }
        startHeartbeat();
    }

    private void startHeartbeat() {
        if (!autoHeartbeat) return;
        synchronized (this) {
            if (heartbeatTimer != null) return;
            heartbeatTimer = new Timer("epic-verify-heartbeat", true);
            heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    doHeartbeat();
                }
            }, heartbeatIntervalMs, heartbeatIntervalMs);
        }
    }

    private void doHeartbeat() {
        try {
            Resp resp = cardValid();
            if (!resp.isSuccess()) {
                if (heartbeatFailListener != null) heartbeatFailListener.onHeartbeatFail(resp);
            }
        } catch (Exception e) {
            if (debug) System.out.println("[EpicVerify] heartbeat error: " + e.getMessage());
            if (heartbeatFailListener != null) {
                Resp err = new Resp();
                err.code = -1;
                err.msg = e.getMessage() == null ? e.toString() : e.getMessage();
                heartbeatFailListener.onHeartbeatFail(err);
            }
        }
    }

    private Resp requestCard(int op, LinkedHashMap<String, String> data) throws EpicVerifyException {
        try {
            byte[] body = buildCardRequest(op, data);
            String json = requestJson(body);
            return parseResp(json);
        } catch (EpicVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new EpicVerifyException("card request failed: " + e.getMessage(), e);
        }
    }

    private Resp requestSoft() throws EpicVerifyException {
        try {
            byte[] body = buildSoftRequest();
            String json = requestJson(body);
            return parseResp(json);
        } catch (EpicVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new EpicVerifyException("soft request failed: " + e.getMessage(), e);
        }
    }

    private byte[] buildBasic() throws IOException {
        Proto p = new Proto();
        if (locale != null && locale.length() > 0) p.writeStringField(1, locale);
        if (deviceId != null && deviceId.length() > 0) p.writeStringField(2, deviceId);
        p.writeInt32Field(3, appVersion);
        if (statusMachine != null && statusMachine.length() > 0) p.writeStringField(4, statusMachine);
        p.writeInt64Field(5, System.currentTimeMillis());
        return p.toByteArray();
    }

    private byte[] buildMapEntry(String key, byte[] value) throws IOException {
        Proto p = new Proto();
        p.writeStringField(1, key);
        p.writeBytesField(2, value);
        return p.toByteArray();
    }

    private byte[] buildSoftRequest() throws IOException {
        byte[] basic = buildBasic();
        Proto soft = new Proto();
        soft.writeBytesField(1, basic);
        if (appKey != null && appKey.length() > 0) {
            soft.writeBytesField(2, buildMapEntry("appkey", appKey.getBytes("UTF-8")));
        }
        if (packageName != null && packageName.length() > 0) {
            soft.writeBytesField(2, buildMapEntry("packageName", packageName.getBytes("UTF-8")));
        }
        Proto msg = new Proto();
        msg.writeInt32Field(1, 0); // DataType.Soft
        msg.writeBytesField(2, soft.toByteArray());
        return msg.toByteArray();
    }

    private byte[] buildCardRequest(int op, LinkedHashMap<String, String> data) throws IOException {
        byte[] basic = buildBasic();
        Proto card = new Proto();
        card.writeBytesField(1, basic);
        card.writeInt32Field(2, op); // Card.opt
        if (appKey != null && appKey.length() > 0) {
            card.writeBytesField(3, buildMapEntry("appkey", appKey.getBytes("UTF-8")));
        }
        if (data != null) {
            for (Map.Entry<String, String> e : data.entrySet()) {
                if (e.getKey() == null) continue;
                byte[] v = e.getValue() == null ? new byte[0] : e.getValue().getBytes("UTF-8");
                card.writeBytesField(3, buildMapEntry(e.getKey(), v));
            }
        }
        Proto msg = new Proto();
        msg.writeInt32Field(1, 1); // DataType.Card
        msg.writeBytesField(3, card.toByteArray());
        return msg.toByteArray();
    }

    private String requestJson(byte[] body) throws EpicVerifyException {
        IOException last = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                byte[] payload = roundTrip(body);
                byte[] uncompressed = GZip.uncompress(payload);
                String rsaBase64 = new String(uncompressed, "UTF-8");
                String json = RSAUtil.decryptWithPublicKey(rsaBase64, publicKey);
                if (debug) System.out.println("[EpicVerify] resp: " + json);
                return json;
            } catch (IOException e) {
                last = e;
                if (attempt < retryCount) {
                    try {
                        Thread.sleep(200L * (attempt + 1));
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (EpicVerifyException e) {
                throw e;
            } catch (Exception e) {
                throw new EpicVerifyException("request failed: " + e.getMessage(), e);
            }
        }
        throw new EpicVerifyException("network error: " + (last == null ? "unknown" : last.getMessage()), last);
    }

    private byte[] roundTrip(byte[] body) throws IOException {
        IOException last = null;
        for (String h : hosts) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(h, port), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                byte[] compressed = GZip.compress(body);
                out.write(MAGIC.getBytes("UTF-8"));
                out.writeInt(TYPE_VERIFY);
                out.writeInt(compressed.length);
                out.write(compressed);
                out.flush();
                byte[] magic = new byte[MAGIC.getBytes("UTF-8").length];
                in.readFully(magic);
                int type = in.readInt();
                if (type != TYPE_VERIFY) throw new IOException("unexpected message type: " + type);
                int len = in.readInt();
                if (len <= 0 || len > MAX_PAYLOAD) throw new IOException("bad payload length: " + len);
                byte[] payload = new byte[len];
                in.readFully(payload);
                lastServer = h;
                if (debug) System.out.println("[EpicVerify] connected to " + h + ":" + port);
                return payload;
            } catch (IOException e) {
                last = e;
                if (debug) System.out.println("[EpicVerify] host " + h + ":" + port + " failed: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        throw last != null ? last : new IOException("no available host");
    }

    private Resp parseResp(String json) throws EpicVerifyException {
        try {
            Json.Value v = Json.parse(json);
            if (!(v instanceof Obj)) throw new EpicVerifyException("resp is not an object");
            Obj o = (Obj) v;
            Resp r = new Resp();
            r.raw = json;
            r.code = o.getInt("code");
            r.msg = o.optString("msg", "");
            r.data = o.getObject("data");
            r.time = o.getLong("time");
            r.layout = o.getObject("layout");
            r.privateTemplate = o.getObject("private_template");
            r.publicTemplate = o.getObject("public_template");
            return r;
        } catch (EpicVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new EpicVerifyException("parse resp failed: " + e.getMessage(), e);
        }
    }

    private Notice parseNotice(Resp soft) throws EpicVerifyException {
        if (soft == null || soft.data == null) return null;
        String info = soft.data.getString("announcementInfo");
        if (info == null || info.length() == 0) return null;
        try {
            Json.Value v = Json.parse(info);
            if (!(v instanceof Obj)) return null;
            Obj o = (Obj) v;
            Notice n = new Notice();
            n.raw = info;
            n.title = pick(o, "title_value", "dialog_title");
            n.content = pick(o, "msg_value", "dialog_msg");
            parseBtn(o, n.confirm, "confirm");
            parseBtn(o, n.cancel, "cancel");
            parseBtn(o, n.extra, "extra");
            n.diffShowEnable = o.getBoolean("diff_show_enable");
            n.announcementMode = o.getInt("announcement_mode");
            n.dialogStyle = o.getInt("dialog_style");
            n.showType = o.getInt("show_type");
            return n;
        } catch (Exception e) {
            throw new EpicVerifyException("parse announcement failed: " + e.getMessage(), e);
        }
    }

    private Version parseVersion(Resp soft, String currentVersion) throws EpicVerifyException {
        if (soft == null || soft.data == null) return null;
        String info = soft.data.getString("upgradeInfo");
        if (info == null || info.length() == 0) return null;
        try {
            Json.Value v = Json.parse(info);
            if (!(v instanceof Obj)) return null;
            Obj o = (Obj) v;
            Version ver = new Version();
            ver.raw = info;
            ver.title = pick(o, "title_value", "dialog_title");
            ver.content = pick(o, "msg_value", "dialog_msg");
            ver.version = o.getInt("upgrade_version");
            ver.downloadUrl = o.getString("upgrade_url");
            ver.updateType = o.getInt("upgrade_type");
            ver.upgradeBtn = pick(o, "upgrade_btn_value", "confirm_btn_value");
            ver.cancelBtn = pick(o, "cancel_btn_value", "cancel_text");
            ver.ignoreBtn = pick(o, "ignore_btn_value", null);
            ver.updateLog = o.getString("update_log");
            if (ver.updateLog == null) ver.updateLog = o.getString("upgrade_log");
            ver.cancelEnable = o.getBoolean("upgrade_cancel_enable");
            ver.ignoreEnable = o.getBoolean("upgrade_ignore_enable");
            ver.dialogStyle = o.getInt("dialog_style");
            ver.showType = o.getInt("show_type");
            int cur = 0;
            if (currentVersion != null) {
                try {
                    cur = Integer.parseInt(currentVersion.trim());
                } catch (Exception ignored) {
                }
            }
            ver.hasUpdate = ver.version > cur;
            return ver;
        } catch (Exception e) {
            throw new EpicVerifyException("parse upgrade failed: " + e.getMessage(), e);
        }
    }

    private VerifyConfig parseVerifyConfig(Resp soft) throws EpicVerifyException {
        if (soft == null || soft.data == null) return null;
        String info = soft.data.getString("verifyInfo");
        if (info == null || info.length() == 0) return null;
        try {
            Json.Value v = Json.parse(info);
            if (!(v instanceof Obj)) return null;
            Obj o = (Obj) v;
            VerifyConfig c = new VerifyConfig();
            c.raw = info;
            c.title = pick(o, "title_value", "dialog_title");
            c.content = pick(o, "msg_value", "dialog_msg");
            c.bindingType = o.getInt("binding_type");
            c.heartbeatType = o.getInt("heartbeat_type");
            c.heartbeatTime = o.getInt("heartbeat_time");
            c.heartbeatEvent = o.getInt("heartbeat_event");
            c.heartbeatEventValue = o.getString("heartbeat_event_value");
            c.cardPlaceholder = o.getString("card_placeholder");
            parseBtn(o, c.confirm, "confirm");
            parseBtn(o, c.cancel, "cancel");
            parseBtn(o, c.extra, "extra");
            c.secretKey = o.getString("secretKey");
            c.dialogStyle = o.getInt("dialog_style");
            c.showType = o.getInt("show_type");
            return c;
        } catch (Exception e) {
            throw new EpicVerifyException("parse verify config failed: " + e.getMessage(), e);
        }
    }

    private Pass parsePass(Resp soft) throws EpicVerifyException {
        if (soft == null || soft.data == null) return null;
        String info = soft.data.getString("passInfo");
        if (info == null || info.length() == 0) return null;
        try {
            Json.Value v = Json.parse(info);
            if (!(v instanceof Obj)) return null;
            Obj o = (Obj) v;
            Pass p = new Pass();
            p.raw = info;
            p.passType = o.getInt("pass_type");
            p.title = pick(o, "title_value", "dialog_title");
            p.content = pick(o, "msg_value", "dialog_msg");
            parseBtn(o, p.confirm, "confirm");
            parseBtn(o, p.cancel, "cancel");
            parseBtn(o, p.extra, "extra");
            p.pass = o.getString("pass");
            p.showType = o.getInt("show_type");
            return p;
        } catch (Exception e) {
            throw new EpicVerifyException("parse pass config failed: " + e.getMessage(), e);
        }
    }

    private static String pick(Obj o, String key, String fallbackKey) {
        String s = o.getString(key);
        if (s == null && fallbackKey != null) s = o.getString(fallbackKey);
        return s;
    }

    /** 解析按钮：文本={prefix}_btn_value，事件={prefix}_btn_event，值={prefix}_btn_event_value（回退 {prefix}_event_value）。 */
    /** 解析按钮：文本={prefix}_btn_value，事件={prefix}_btn_event，值={prefix}_btn_event_value（回退 {prefix}_event_value）。 */
    private static void parseBtn(Obj o, Btn b, String prefix) {
        b.text = o.getString(prefix + "_btn_value");
        b.event = o.getInt(prefix + "_btn_event");
        b.value = o.getString(prefix + "_btn_event_value");
        if (b.value == null) b.value = o.getString(prefix + "_event_value");
    }
}
