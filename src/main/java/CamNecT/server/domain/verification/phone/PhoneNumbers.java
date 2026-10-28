package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;

public final class PhoneNumbers {
    private PhoneNumbers() {}

    public static boolean isValid(String value) {
        return value != null && value.matches("010[0-9]{8}");
    }

    public static String normalize(String value) {
        if (value == null || value.length() > 20) throw new CustomException(PhoneErrorCode.INVALID_PHONE);
        String number = value.trim().replace("-", "").replace(" ", "");
        if (number.startsWith("+82")) number = "0" + number.substring(3);
        if (!number.matches("010[0-9]{8}")) throw new CustomException(PhoneErrorCode.INVALID_PHONE);
        return number;
    }
}
