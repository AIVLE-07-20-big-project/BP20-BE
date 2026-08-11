package com.bp20.backend.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDataMaskerTest {

    @Test
    void masksPersonalDataForManagementScreens() {
        assertThat(PersonalDataMasker.email("store-owner@bp20.com"))
                .isEqualTo("st*********@bp20.com");
        assertThat(PersonalDataMasker.name("홍길동"))
                .isEqualTo("홍*동");
        assertThat(PersonalDataMasker.phoneNumber("010-1234-5678"))
                .isEqualTo("010****5678");
        assertThat(PersonalDataMasker.businessNumber("123-45-67890"))
                .isEqualTo("123-**-*****");
        assertThat(PersonalDataMasker.ipAddress("203.0.113.25"))
                .isEqualTo("203.0.113.***");
    }

    @Test
    void safelyMasksInvalidBusinessNumber() {
        assertThat(PersonalDataMasker.businessNumber("invalid"))
                .isEqualTo("***-**-*****");
    }
}
