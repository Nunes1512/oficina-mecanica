package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficina.exception.NegocioException;
import com.oficina.model.StatusVeiculo;
import org.junit.jupiter.api.Test;

class FluxoStatusVeiculoTest {

    @Test
    void ordemDasEtapasEhADoEnunciado() {
        assertThat(StatusVeiculo.values()).containsExactly(
                StatusVeiculo.AGUARDANDO_ANALISE, StatusVeiculo.AGUARDANDO_PECAS, StatusVeiculo.EM_MANUTENCAO,
                StatusVeiculo.TESTES_FINAIS, StatusVeiculo.CONCLUIDO, StatusVeiculo.ENTREGUE);
        assertThat(StatusVeiculo.TESTES_FINAIS.getRotulo()).isEqualTo("Em teste");
    }

    @Test
    void funcionarioAvancaUmaEtapaPorVez() {
        assertThatCode(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.AGUARDANDO_ANALISE, StatusVeiculo.AGUARDANDO_PECAS, false)).doesNotThrowAnyException();
    }

    @Test
    void naoPulaEtapa() {
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.AGUARDANDO_ANALISE, StatusVeiculo.CONCLUIDO, true))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("ordem");
    }

    @Test
    void funcionarioNaoRegistraAEntrega() {
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.CONCLUIDO, StatusVeiculo.ENTREGUE, false))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("gerente");
    }

    @Test
    void gerenteRegistraAEntrega() {
        assertThatCode(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.CONCLUIDO, StatusVeiculo.ENTREGUE, true)).doesNotThrowAnyException();
    }

    @Test
    void funcionarioNaoVoltaEtapa() {
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.EM_MANUTENCAO, StatusVeiculo.AGUARDANDO_PECAS, false))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void gerenteVoltaUmaEtapaParaCorrigir() {
        assertThatCode(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.EM_MANUTENCAO, StatusVeiculo.AGUARDANDO_PECAS, true)).doesNotThrowAnyException();
    }

    @Test
    void funcionarioNaoDesfazAEntrega() {
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.ENTREGUE, StatusVeiculo.CONCLUIDO, false)).isInstanceOf(NegocioException.class);
    }

    @Test
    void recusaStatusIgualOuNulo() {
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.EM_MANUTENCAO, StatusVeiculo.EM_MANUTENCAO, true)).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> FluxoStatusVeiculo.validar(
                StatusVeiculo.EM_MANUTENCAO, null, true)).isInstanceOf(NegocioException.class);
    }

    @Test
    void botoesDisponiveisDependemDoPerfil() {
        assertThat(FluxoStatusVeiculo.proximoPara(StatusVeiculo.CONCLUIDO, false)).isNull();
        assertThat(FluxoStatusVeiculo.proximoPara(StatusVeiculo.CONCLUIDO, true)).isEqualTo(StatusVeiculo.ENTREGUE);
        assertThat(FluxoStatusVeiculo.proximoPara(StatusVeiculo.EM_MANUTENCAO, false)).isEqualTo(StatusVeiculo.TESTES_FINAIS);
        assertThat(FluxoStatusVeiculo.proximoPara(StatusVeiculo.ENTREGUE, true)).isNull();
        assertThat(FluxoStatusVeiculo.anteriorPara(StatusVeiculo.EM_MANUTENCAO, false)).isNull();
        assertThat(FluxoStatusVeiculo.anteriorPara(StatusVeiculo.EM_MANUTENCAO, true)).isEqualTo(StatusVeiculo.AGUARDANDO_PECAS);
        assertThat(FluxoStatusVeiculo.anteriorPara(StatusVeiculo.AGUARDANDO_ANALISE, true)).isNull();
    }
}
