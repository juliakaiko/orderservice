package com.mymicroservice.orderservice.service;

import java.time.LocalDate;

public interface PartitionService {
    void createPartitionForDate(LocalDate date);
}
