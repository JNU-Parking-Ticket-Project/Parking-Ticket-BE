package com.jnu.ticketapi;


import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STREAM;

import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
public class RestDocsConfig {
    protected MockMvc mvc;
    protected RestDocumentationResultHandler document;

    @BeforeEach
    public void setup(
            WebApplicationContext webApplicationContext,
            RestDocumentationContextProvider restDocumentation) {
        clearRedisState(webApplicationContext);
        this.document =
                MockMvcRestDocumentation.document(
                        "{class-name}/{method-name}",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()));

        mvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .addFilter(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true))
                        .apply(
                                MockMvcRestDocumentation.documentationConfiguration(
                                        restDocumentation))
                        // .apply(SecurityMockMvcConfigurers.springSecurity())
                        .alwaysDo(document)
                        .build();
    }

    private void clearRedisState(WebApplicationContext webApplicationContext) {
        webApplicationContext
                .getBeanProvider(RedisRepository.class)
                .ifAvailable(
                        redisRepository -> {
                            redisRepository.delete(REDIS_EVENT_ISSUE_STREAM);
                            redisRepository.deleteKeysByPrefix("parking-ticket:event:");
                        });
    }
}
