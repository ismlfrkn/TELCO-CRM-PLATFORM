package com.turkcell.customer.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCKN checksum algoritmasi (11 haneli kimlik no dogrulama) yanlis yazilirsa gecerli musteriler
 * reddedilir ya da gecersiz kimlikler kabul edilir - bu yuzden algoritmanin kendisi izole test edilir.
 * "10000000146" Turkiye'de yaygin kullanilan, algoritmik olarak gecerli bir test TCKN'sidir
 * (gercek bir kisiye ait degildir).
 */
class IdentityNumberValidatorTest {

    @Test
    void isValidTckn_withKnownValidChecksum_returnsTrue() {
        assertThat(IdentityNumberValidator.isValidTckn("10000000146")).isTrue();
    }

    @Test
    void isValidTckn_withWrongCheckDigit_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidTckn("10000000147")).isFalse();
    }

    @Test
    void isValidTckn_withLeadingZero_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidTckn("01000000146")).isFalse();
    }

    @Test
    void isValidTckn_withWrongLength_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidTckn("123456789")).isFalse();
        assertThat(IdentityNumberValidator.isValidTckn("123456789012")).isFalse();
    }

    @Test
    void isValidTckn_withNonDigitCharacters_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidTckn("1000000014A")).isFalse();
    }

    @Test
    void isValidTckn_withNull_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidTckn(null)).isFalse();
    }

    @Test
    void isValidVkn_withValidFormat_returnsTrue() {
        assertThat(IdentityNumberValidator.isValidVkn("1234567890")).isTrue();
    }

    @Test
    void isValidVkn_withLeadingZero_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidVkn("0234567890")).isFalse();
    }

    @Test
    void isValidVkn_withWrongLength_returnsFalse() {
        assertThat(IdentityNumberValidator.isValidVkn("123456789")).isFalse();
    }

    @Test
    void isValid_withIndividualType_delegatesToTcknCheck() {
        assertThat(IdentityNumberValidator.isValid("INDIVIDUAL", "10000000146")).isTrue();
        assertThat(IdentityNumberValidator.isValid("INDIVIDUAL", "1234567890")).isFalse();
    }

    @Test
    void isValid_withCorporateType_delegatesToVknCheck() {
        assertThat(IdentityNumberValidator.isValid("CORPORATE", "1234567890")).isTrue();
        assertThat(IdentityNumberValidator.isValid("CORPORATE", "10000000146")).isFalse();
    }

    @Test
    void isValid_withUnknownType_returnsFalse() {
        assertThat(IdentityNumberValidator.isValid("GOVERNMENT", "10000000146")).isFalse();
    }
}
