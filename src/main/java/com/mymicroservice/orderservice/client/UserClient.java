package com.mymicroservice.orderservice.client;

import com.mymicroservice.orderservice.configuration.UserClientConfig;
import com.mymicroservice.orderservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "userservice", url = "${user-service.url}",configuration = UserClientConfig.class)
public interface UserClient {

    @GetMapping("/api/users/find-by-email")  // mapping to endpoint of userservice "getUserByEmail()"
    UserResponse getUserByEmail(@RequestParam String email);

    @GetMapping("/api/users/{id}")  // mapping to endpoint of userservice "getUserById()"
    UserResponse getUserById(@PathVariable("id") Long id);

}
