package CamNecT.server.global.common.util;

public final class PhoneNumbers {
    public static final String MOBILE_PATTERN = "01(?:0[0-9]{8}|[16789][0-9]{7,8})";

    private PhoneNumbers() {}

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replace("-", "").replace(" ", "");
    }

    public static boolean isValid(String value) {
        return value != null && value.matches(MOBILE_PATTERN);
    }
}
