package com.jnu.ticketapi.ticket;


import com.jnu.ticketapi.Announce.config.DatabaseClearExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
public class CreateTicketTest {
    //  Either
    //  @Test()
}
