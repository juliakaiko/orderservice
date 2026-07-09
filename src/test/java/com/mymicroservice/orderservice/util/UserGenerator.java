package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.dto.UserDto;

import java.time.LocalDate;

import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_EMAIL;
import static com.mymicroservice.orderservice.util.data.TestConstants.TEST_USER_ID;

public class UserGenerator {

    public static UserDto generateUserResponse() {
        return UserDto.builder()
                .userId(TEST_USER_ID)
                .name("TestName")
                .surname("TestSurName")
                .birthDate(LocalDate.of(2000, 2, 2))
                .email(TEST_USER_EMAIL)
                .build();
    }
}
