package com.mymicroservice.orderservice.client;

import com.mymicroservice.orderservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "userservice", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/api/internal/users/find-by-email")  // mapping to endpoint of userservice "getUserByEmail()"
    UserDto getUserByEmail(@RequestParam String email);

    @GetMapping("/api/internal/users/{id}")  // mapping to endpoint of userservice "getUserById()"
    UserDto getUserById(@PathVariable("id") Long id);

}
