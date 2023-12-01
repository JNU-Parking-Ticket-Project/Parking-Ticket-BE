package com.jnu.ticketapi;


import com.jnu.ticketapi.user.supports.ApiIntegrateSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ApiIntegrateSpringBootTest
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
// @Sql("classpath:db/teardown.sql")
@AutoConfigureMockMvc
class TicketApiApplicationTests {
    @Test
    void contextLoads() {}
}
