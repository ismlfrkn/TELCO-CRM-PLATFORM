package com.turkcell.customer.validation;

public final class IdentityNumberValidator {

    private IdentityNumberValidator() {
    }

    public static boolean isValid(String customerType, String identityNumber) {
        if ("INDIVIDUAL".equals(customerType)) {
            return isValidTckn(identityNumber);
        }
        if ("CORPORATE".equals(customerType)) {
            return isValidVkn(identityNumber);
        }
        return false;
    }

    public static boolean isValidTckn(String tckn) {
        if (tckn == null || !tckn.matches("\\d{11}") || tckn.charAt(0) == '0') {
            return false;
        }

        int[] digits = tckn.chars().map(c -> c - '0').toArray();
        int oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int evenSum = digits[1] + digits[3] + digits[5] + digits[7];

        int digit10 = Math.floorMod((oddSum * 7) - evenSum, 10);
        int sumFirst10 = 0;
        for (int i = 0; i < 10; i++) {
            sumFirst10 += digits[i];
        }
        int digit11 = sumFirst10 % 10;

        return digit10 == digits[9] && digit11 == digits[10];
    }

    public static boolean isValidVkn(String vkn) {
        return vkn != null && vkn.matches("\\d{10}") && vkn.charAt(0) != '0';
    }
}
