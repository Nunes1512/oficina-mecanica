package com.oficina.form;

import com.oficina.model.Peca;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Os saldos nao sao editaveis neste cadastro: apenas por movimentacoes de estoque. */
@Getter
@Setter
public class PecaForm {
    @NotBlank(message = "Informe o nome da peça.")
    @Size(max = 120, message = "Nome limitado a 120 caracteres.")
    private String nome;

    @Size(max = 80, message = "Fabricante limitado a 80 caracteres.")
    private String fabricante;

    @NotNull(message = "Informe o preço de venda de referência.")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Preço inválido (até 8 inteiros e 2 decimais).")
    private BigDecimal valorUnitario;

    @NotNull(message = "Informe o estoque mínimo.")
    @Min(value = 0, message = "Estoque mínimo não pode ser negativo.")
    @Max(value = 1000000, message = "Estoque mínimo muito alto.")
    private Integer estoqueMinimo = 0;

    public static PecaForm de(Peca peca) {
        PecaForm f = new PecaForm();
        f.setNome(peca.getNome());
        f.setFabricante(peca.getFabricante());
        f.setValorUnitario(peca.getValorUnitario());
        f.setEstoqueMinimo(peca.getEstoqueMinimo());
        return f;
    }
}
