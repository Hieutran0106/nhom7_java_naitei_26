package com.nhom7.coworkingspace.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        com.nhom7.coworkingspace.CoworkingSpaceApplication.class,
        SecurityConfigTest.TestControllerConfig.class
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * ADMIN phải truy cập được API của MODERATOR.
     * Đây là case chính của feature Admin inherit from Moderator.
     */
    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void adminShouldAccessModeratorApi() throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Moderator API"));
    }

    /**
     * MODERATOR vẫn phải truy cập được API của chính mình.
     */
    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    void moderatorShouldAccessModeratorApi() throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Moderator API"));
    }

    /**
     * USER không được phép truy cập API của MODERATOR.
     */
    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = "USER"
    )
    void userShouldNotAccessModeratorApi() throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(status().isForbidden());
    }

    /**
     * HOST không được phép truy cập API của MODERATOR.
     */
    @Test
    @WithMockUser(
            username = "host@test.com",
            roles = "HOST"
    )
    void hostShouldNotAccessModeratorApi() throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(status().isForbidden());
    }

    /**
     * Người chưa đăng nhập không được truy cập API của MODERATOR.
     *
     * Với JwtAuthErrorHandler hiện tại của project,
     * request chưa authenticate có thể trả về 403.
     */
    @Test
    void unauthenticatedUserShouldNotAccessModeratorApi() throws Exception {

        mockMvc.perform(
                        get("/api/moderator/test")
                )
                .andExpect(status().isForbidden());
    }

    /**
     * MODERATOR không được phép truy cập API riêng của ADMIN.
     *
     * Test này đảm bảo quan hệ quyền chỉ theo một chiều:
     *
     * ADMIN -> MODERATOR : được
     * MODERATOR -> ADMIN : không được
     */
    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = "MODERATOR"
    )
    void moderatorShouldNotAccessAdminApi() throws Exception {

        mockMvc.perform(
                        get("/api/admin/test")
                )
                .andExpect(status().isForbidden());
    }

    /**
     * Đăng ký controller giả chỉ dùng trong test.
     */
    @TestConfiguration
    static class TestControllerConfig {

        @Bean
        TestModeratorController testModeratorController() {
            return new TestModeratorController();
        }
    }

    /**
     * Controller giả để kiểm tra SecurityConfig.
     *
     * Không ảnh hưởng đến code production.
     */
    @RestController
    static class TestModeratorController {

        @GetMapping("/api/moderator/test")
        String moderatorApi() {
            return "Moderator API";
        }

        @GetMapping("/api/admin/test")
        String adminApi() {
            return "Admin API";
        }
    }
}