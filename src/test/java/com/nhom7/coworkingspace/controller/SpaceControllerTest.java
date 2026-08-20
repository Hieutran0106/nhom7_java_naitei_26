package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.SpaceController;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.SpaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceController.class)
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("SpaceController - Integration & Security Tests")
class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpaceService spaceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> GET /api/spaces/search returns 200 OK")
    void givenUserRole_whenSearchSpaces_thenReturn200() throws Exception {
        SpaceResponse responseDto = SpaceResponse.builder()
                .id(1L)
                .name("Desk 101")
                .type("working desk")
                .price(new BigDecimal("150000.00"))
                .priceUnit("day")
                .venueCity("Da Nang")
                .build();

        PageResponse<SpaceResponse> pageResponse = PageResponse.<SpaceResponse>builder()
                .content(List.of(responseDto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(spaceService.searchSpaces(any(SpaceSearchRequest.class))).willReturn(pageResponse);

        mockMvc.perform(get("/api/spaces/search")
                        .param("name", "Desk")
                        .param("city", "Da Nang")
                        .param("type", "working desk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Fetched co-working spaces successfully"))
                .andExpect(jsonPath("$.data.content[0].name").value("Desk 101"))
                .andExpect(jsonPath("$.data.content[0].type").value("working desk"))
                .andExpect(jsonPath("$.data.content[0].venueCity").value("Da Nang"));
    }

    @Test
    @DisplayName("Unauthenticated request -> GET /api/spaces/search returns 401 Unauthorized")
    void givenUnauthenticated_whenSearchSpaces_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/spaces/search"))
                .andExpect(status().isUnauthorized());
    }
}
