package com.oficina.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Exige PostgreSQL + Flyway. Rodar em banco isolado (CI), nunca no banco de produção. */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegracaoTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username="mecanico-teste", roles="MECANICO")
    void mecanicoNaoListaVeiculos() throws Exception {
        mvc.perform(get("/veiculos")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="assistente-teste", roles="ASSISTENTE_ADMINISTRATIVO")
    void assistenteNaoVeDashboardFinanceiro() throws Exception {
        mvc.perform(get("/api/dashboard/financeiro")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="gerente-teste", roles="GERENTE")
    void gerenteConsultaCatalogo() throws Exception {
        mvc.perform(get("/pecas")).andExpect(status().isOk());
    }
}
