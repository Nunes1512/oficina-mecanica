package com.oficina.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntradaEstoqueForm {
    @NotNull(message = "Informe a quantidade recebida.")
    @Min(value = 1, message = "A quantidade deve ser positiva.")
    @Max(value = 1000000, message = "Quantidade muito alta.")
    private Integer quantidade;

    /** Custo de aquisicao, separado do preco de venda da peca. */
    @NotNull(message = "Informe o custo unitário de compra.")
    @DecimalMin(value = "0.00", message = "O custo não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Custo inválido (até 8 inteiros e 2 decimais).")
    private BigDecimal custoUnitario;

    @Size(max = 200, message = "Observação limitada a 200 caracteres.")
    private String observacao;
}
