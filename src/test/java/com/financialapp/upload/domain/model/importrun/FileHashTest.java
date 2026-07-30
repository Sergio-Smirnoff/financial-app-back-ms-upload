package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.exception.InvalidFileHashException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileHashTest {

    @Test
    void shouldComputeSha256ForTestString() {
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        FileHash hash = FileHash.ofBytes(bytes);

        assertThat(hash.value()).isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08", // Uppercase rejected
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a0",   // 63 chars
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a089", // 65 chars
            "zzzzd081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", // Non-hex
            ""
    })
    void shouldRejectInvalidFileHashValues(String invalidHash) {
        assertThatThrownBy(() -> new FileHash(invalidHash))
                .isInstanceOf(InvalidFileHashException.class);
    }
}
