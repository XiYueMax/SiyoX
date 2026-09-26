package XiYue.SiyoX;

public final class SiyoXEntityKillerConfig {

    public static boolean ENABLE_CUSTOM_ENTITY_KILLER = false;

    public static String CUSTOM_ENTITY_KILLER_PATTERNS =
            SiyoXConfig.DEFAULT_ENTITY_KILLER_PATTERNS;

    static {
        loadNativeEntityKillerConfig();
    }

    private SiyoXEntityKillerConfig() {
    }

    public static void loadNativeEntityKillerConfig() {
        try {
            if (!XiYue.SiyoX.data.NativeVerify.isNativeLoaded()) {
                return;
            }
            ENABLE_CUSTOM_ENTITY_KILLER =
                    XiYue.SiyoX.data.NativeVerify.nativeGetEnableCustomEntityKiller();

            String nativePatterns =
                    XiYue.SiyoX.data.NativeVerify.nativeGetEntityKillerPatterns();
            if (nativePatterns != null && !nativePatterns.trim().isEmpty()) {
                CUSTOM_ENTITY_KILLER_PATTERNS = nativePatterns.trim();
            }
        } catch (Throwable t) {

            t.printStackTrace();
        }
    }

    public static String getEffectivePatterns() {
        if (ENABLE_CUSTOM_ENTITY_KILLER && CUSTOM_ENTITY_KILLER_PATTERNS != null && !CUSTOM_ENTITY_KILLER_PATTERNS.trim().isEmpty()) {
            return CUSTOM_ENTITY_KILLER_PATTERNS.trim();
        }
        if (SiyoXConfig.ENTITY_KILLER_PATTERNS != null && !SiyoXConfig.ENTITY_KILLER_PATTERNS.trim().isEmpty()) {
            return SiyoXConfig.ENTITY_KILLER_PATTERNS.trim();
        }
        return SiyoXConfig.DEFAULT_ENTITY_KILLER_PATTERNS;
    }
}
