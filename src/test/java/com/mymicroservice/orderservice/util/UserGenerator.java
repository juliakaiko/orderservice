package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.dto.UserDto;
import java.time.LocalDate;

public class UserGenerator {

    public static UserDto generateUserResponse() {

        return  UserDto.builder()
                .userId(1l)
                .name("test_name")
                .surname("test_surname")
                .birthDate(LocalDate.of(2000, 2, 2))
                .email ("test@test.by")
                .build();
    }
}
