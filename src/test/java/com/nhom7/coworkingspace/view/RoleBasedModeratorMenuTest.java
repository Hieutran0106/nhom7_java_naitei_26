package com.nhom7.coworkingspace.view;

import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleBasedModeratorMenuTest.MenuPreviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RoleBasedModeratorMenuTest.MenuPreviewController.class)
class RoleBasedModeratorMenuTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminSeesEveryModeratorMenuItem() throws Exception {
        assertModeratorMenuIsVisible(renderMenu());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorSeesEveryModeratorMenuItem() throws Exception {
        assertModeratorMenuIsVisible(renderMenu());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserDoesNotSeeModeratorMenu() throws Exception {
        String html = renderMenu();

        assertThat(html).doesNotContain("data-menu=\"moderator\"");
        assertThat(html).doesNotContain("data-menu-item=\"users\"");
        assertThat(html).doesNotContain("data-menu-item=\"venues\"");
        assertThat(html).doesNotContain("data-menu-item=\"bookings\"");
    }

    private String renderMenu() throws Exception {
        return mockMvc.perform(get("/test/moderator-menu"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void assertModeratorMenuIsVisible(String html) {
        assertThat(html).contains("data-menu=\"moderator\"");
        assertThat(html).contains("data-menu-item=\"users\"");
        assertThat(html).contains("data-menu-item=\"venues\"");
        assertThat(html).contains("data-menu-item=\"bookings\"");
    }

    @Controller
    static class MenuPreviewController {

        @GetMapping("/test/moderator-menu")
        String renderMenu() {
            return "fragments/moderator-menu";
        }
    }
}
