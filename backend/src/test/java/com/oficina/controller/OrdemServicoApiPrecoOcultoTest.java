package com.oficina.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.oficina.model.ItemOrcamento;
import com.oficina.model.Peca;
import com.oficina.model.TipoItemOrcamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrdemServicoApiPrecoOcultoTest {
    @Test
    void precoDoOrcamentoNaoEExpostoAoMecanico() {
        Peca peca = new Peca();
        peca.setId(5L);
        ItemOrcamento item = new ItemOrcamento();
        item.setId(99L);
        item.setPeca(peca);
        item.setDescricao("Troca de bateria");
        item.setTipo(TipoItemOrcamento.PECA);
        item.setQuantidade(1);
        item.setValorUnitario(new BigDecimal("489.00"));

        OrdemServicoApiController.ItemResposta mecanico =
                OrdemServicoApiController.ItemResposta.de(item, false);
        OrdemServicoApiController.ItemResposta gerente =
                OrdemServicoApiController.ItemResposta.de(item, true);

        assertNull(mecanico.valorUnitario());
        assertEquals(new BigDecimal("489.00"), gerente.valorUnitario());
        assertEquals(Long.valueOf(5L), mecanico.pecaId());
        assertEquals(Integer.valueOf(1), mecanico.quantidade());
    }
}
