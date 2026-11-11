package com.oficina.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.oficina.exception.NegocioException;
import org.junit.jupiter.api.Test;

class TextosTest {
    @Test void limparTrocaVazioPorNulo() {
        assertThat(Textos.limpar(null)).isNull();
        assertThat(Textos.limpar("   ")).isNull();
        assertThat(Textos.limpar("  oi ")).isEqualTo("oi");
    }
    @Test void cpfSemPontuacaoViraFormatoDoBanco() {
        assertThat(Textos.cpf("12345678909")).isEqualTo("123.456.789-09");
    }
    @Test void cpfComPontuacaoContinuaIgual() {
        assertThat(Textos.cpf("123.456.789-09")).isEqualTo("123.456.789-09");
    }
    @Test void cpfComTamanhoErradoEhRecusado() {
        assertThatThrownBy(() -> Textos.cpf("123")).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> Textos.cpf(null)).isInstanceOf(NegocioException.class);
    }
    @Test void cpfComDigitoInvalidoEhRecusado() {
        assertThatThrownBy(() -> Textos.cpf("00000000000")).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> Textos.cpf("12345678900")).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> Textos.cpf("11111111111")).isInstanceOf(NegocioException.class);
    }
    @Test void placaFicaEmMaiusculas() {
        assertThat(Textos.placa(" abc1d23 ")).isEqualTo("ABC1D23");
        assertThat(Textos.placa(null)).isNull();
    }
}
