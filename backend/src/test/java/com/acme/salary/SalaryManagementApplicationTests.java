package com.acme.salary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.seed.enabled=false")
class SalaryManagementApplicationTests {
    @Test
    void contextLoads() {
    }
}
