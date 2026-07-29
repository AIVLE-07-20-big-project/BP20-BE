package com.bp20.backend.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilsTest {

    @Test
    void 하이픈이_포함된_전화번호를_숫자로_정규화한다() {
        assertThat(PhoneNumberUtils.normalize("010-1234-5678")).isEqualTo("01012345678");
        assertThat(PhoneNumberUtils.normalize("02-1234-5678")).isEqualTo("0212345678");
    }

    @Test
    void 비어있는_전화번호는_null로_정규화한다() {
        assertThat(PhoneNumberUtils.normalize(null)).isNull();
        assertThat(PhoneNumberUtils.normalize("  ")).isNull();
    }

    @Test
    void 지원하는_국내_전화번호_형식만_허용한다() {
        assertThat("010-1234-5678").matches(PhoneNumberUtils.OPTIONAL_KOREAN_PHONE_PATTERN);
        assertThat("0212345678").matches(PhoneNumberUtils.OPTIONAL_KOREAN_PHONE_PATTERN);
        assertThat("02-111").doesNotMatch(PhoneNumberUtils.OPTIONAL_KOREAN_PHONE_PATTERN);
        assertThat("123-4567-8901").doesNotMatch(PhoneNumberUtils.OPTIONAL_KOREAN_PHONE_PATTERN);
    }
}
