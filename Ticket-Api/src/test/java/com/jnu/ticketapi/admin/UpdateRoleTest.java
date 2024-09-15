//package com.jnu.ticketapi.admin;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jnu.ticketapi.RestDocsConfig;
//import com.jnu.ticketapi.security.JwtGenerator;
//import com.jnu.ticketdomain.domains.admin.exception.AdminErrorCode;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.jdbc.Sql;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
//
//@Slf4j
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
//@ActiveProfiles("test")
//@AutoConfigureMockMvc
//@AutoConfigureRestDocs
//@Sql("classpath:db/teardown.sql")
//public class UpdateRoleTest extends RestDocsConfig {
//    @Autowired private MockMvc mvc;
//
//    @Autowired private ObjectMapper om;
//
//    @Autowired private JwtGenerator jwtGenerator;
//
//    @Nested
//    class updateRoleTest {
//        @Test
//        @DisplayName("성공 : 권한 변경")
//        void success() throws Exception {
//            {
//                // given
//                Long userId = 1L;
//                String jsonRequest = "{\"role\":\"COUNCIL\"}";
//                String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");
//                log.info("requestBody : " + jsonRequest);
//
//                // when
//                ResultActions resultActions =
//                        mvc.perform(
//                                put("/v1/admin/role/" + userId)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header("Authorization", "Bearer " + accessToken)
//                                        .content(jsonRequest));
//
//                // eye
//                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
//                log.info("responseBody : " + responseBody);
//
//                // then
//                resultActions.andExpectAll(
//                        status().isOk(),
//                        jsonPath("$.userId").value(1L),
//                        jsonPath("$.role").value("COUNCIL"));
//                resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
//            }
//        }
//
//        @Test
//        @DisplayName("실패 : 권한 변경(존재하지 않는 유저 ID 입력)")
//        void fail() throws Exception {
//            {
//                // given
//                Long userId = 55L;
//                String jsonRequest = "{\"role\":\"COUNCIL\"}";
//                String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");
//                log.info("requestBody : " + jsonRequest);
//
//                // when
//                ResultActions resultActions =
//                        mvc.perform(
//                                put("/v1/admin/role/" + userId)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header("Authorization", "Bearer " + accessToken)
//                                        .content(jsonRequest));
//
//                // eye
//                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
//                log.info("responseBody : " + responseBody);
//
//                // then
//                resultActions.andExpectAll(
//                        status().isNotFound(),
//                        jsonPath("$.success").value(false),
//                        jsonPath("$.reason").value("존재하지 않는 유저 입니다."),
//                        jsonPath("$.code").value("USER_404_1"));
//                resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
//            }
//        }
//
//        @Test
//        @DisplayName("실패 : 권한 변경(존재하지 않는 권한 입력)")
//        void fail2() throws Exception {
//            {
//                // given
//                Long userId = 1L;
//                String jsonRequest = "{\"role\":\"WhySoSerious\"}";
//                String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");
//                log.info("requestBody : " + jsonRequest);
//
//                // when
//                ResultActions resultActions =
//                        mvc.perform(
//                                put("/v1/admin/role/" + userId)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header("Authorization", "Bearer " + accessToken)
//                                        .content(jsonRequest));
//
//                // eye
//                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
//                log.info("responseBody : " + responseBody);
//
//                // then
//                resultActions.andExpectAll(
//                        status().isBadRequest(),
//                        jsonPath("$.success").value(false),
//                        jsonPath("$.reason").value("ROLE을 정확히 입력해 주세요."),
//                        jsonPath("$.code").value("BAD_REQUEST"));
//                resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
//            }
//        }
//
//        @Test
//        @DisplayName("실패 : 권한 변경(관리자가 이미 존재하는 경우)")
//        void fail3() throws Exception {
//            {
//                // given
//                Long userId = 1L;
//                String jsonRequest = "{\"role\":\"ADMIN\"}";
//                String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");
//                log.info("requestBody : " + jsonRequest);
//
//                // when
//                ResultActions resultActions =
//                        mvc.perform(
//                                put("/v1/admin/role/" + userId)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header("Authorization", "Bearer " + accessToken)
//                                        .content(jsonRequest));
//
//                // eye
//                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
//                log.info("responseBody : " + responseBody);
//
//                // then
//                resultActions.andExpectAll(
//                        status().isBadRequest(),
//                        jsonPath("$.success").value(false),
//                        jsonPath("$.reason").value("ADMIN이 이미 존재합니다."),
//                        jsonPath("$.code").value("ADMIN_400_1"));
//                resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
//            }
//        }
//
//        @Test
//        @DisplayName("실패 : 권한 변경(자기 자신의 권한 변경)")
//        void fail4() throws Exception {
//            {
//                // given
//                Long userId = 2L;
//                String jsonRequest = "{\"role\":\"COUNCIL\"}";
//                String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");
//                log.info("requestBody : " + jsonRequest);
//
//                // when
//                ResultActions resultActions =
//                        mvc.perform(
//                                put("/v1/admin/role/" + userId)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .header("Authorization", "Bearer " + accessToken)
//                                        .content(jsonRequest));
//
//                // eye
//                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
//                log.info("responseBody : " + responseBody);
//
//                // then
//                resultActions.andExpectAll(
//                        status().isBadRequest(),
//                        jsonPath("$.success").value(false),
//                        jsonPath("$.reason")
//                                .value(AdminErrorCode.NOT_ALLOW_UPDATE_OWN_ROLE.getReason()),
//                        jsonPath("$.code")
//                                .value(AdminErrorCode.NOT_ALLOW_UPDATE_OWN_ROLE.getCode()));
//                resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
//            }
//        }
//    }
//}
