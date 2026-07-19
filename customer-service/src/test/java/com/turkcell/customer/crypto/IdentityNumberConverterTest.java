package com.turkcell.customer.crypto;

import com.turkcell.customer.config.PiiEncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityNumberConverterTest {

    private IdentityNumberConverter converter;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new Random(42).nextBytes(key);
        PiiEncryptionProperties properties = new PiiEncryptionProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(key));
        converter = new IdentityNumberConverter(properties);
    }

    @Test
    void convertToDatabaseColumn_thenBack_roundTripsToOriginalValue() {
        String plainText = "10000000146";

        String cipherText = converter.convertToDatabaseColumn(plainText);
        String decrypted = converter.convertToEntityAttribute(cipherText);

        assertThat(cipherText).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void convertToDatabaseColumn_calledTwiceWithSameInput_producesIdenticalCiphertext() {
        String plainText = "10000000146";

        String first = converter.convertToDatabaseColumn(plainText);
        String second = converter.convertToDatabaseColumn(plainText);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void convertToDatabaseColumn_withDifferentInputs_producesDifferentCiphertext() {
        String first = converter.convertToDatabaseColumn("10000000146");
        String second = converter.convertToDatabaseColumn("22222222222");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void convertToDatabaseColumn_withNull_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_withNull_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
