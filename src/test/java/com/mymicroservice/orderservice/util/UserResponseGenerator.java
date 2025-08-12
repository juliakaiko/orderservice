package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.dto.UserResponse;
import java.time.LocalDate;

public class UserResponseGenerator {

    public static UserResponse generateUserResponse() {

        return  UserResponse.builder()
                .userId(1l)
                .name("test_name")
                .surname("test_surname")
                .birthDate(LocalDate.of(2000, 2, 2))
                .email ("test@test.by")
                .build();
    }
}
