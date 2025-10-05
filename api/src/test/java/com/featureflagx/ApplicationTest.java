package com.featureflagx;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class, TestRedisConfiguration.class})
@ActiveProfiles("test")
class ApplicationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}
