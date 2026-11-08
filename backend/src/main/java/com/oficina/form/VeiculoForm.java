package com.oficina.form;

import com.oficina.model.Veiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * O status NÃO está neste formulário de propósito: ele só muda pelo botão de avançar etapa
 * da tela de detalhes, que obedece à ordem do fluxo e às permissões de cada perfil.
 */
@Getter
@Setter
public class VeiculoForm {

    @NotBlank(message = "Informe a marca.")
    @Size(max = 50, message = "Use no máximo 50 caracteres.")
    private String marca;

    @NotBlank(message = "Informe o modelo.")
    @Size(max = 60, message = "Use no máximo 60 caracteres.")
    private String modelo;

    @NotNull(message = "Informe o ano.")
    @Min(value = 1900, message = "Ano inválido.")
    @Max(value = 2100, message = "Ano inválido.")
    private Integer ano;

    @Size(max = 30, message = "Use no máximo 30 caracteres.")
    private String cor;

    @NotBlank(message = "Informe a placa.")
    @Pattern(regexp = "^[A-Za-z]{3}\\d[A-Za-z0-9]\\d{2}$", message = "Placa inválida. Exemplo: ABC1D23.")
    private String placa;

    @Min(value = 0, message = "A quilometragem não pode ser negativa.")
    private Integer quilometragem;

    @NotNull(message = "Selecione o cliente.")
    private Long clienteId;

    /** Quando o veículo chegou à oficina: define a ordem da fila de atendimento. */
    @NotNull(message = "Informe a data de chegada.")
    @PastOrPresent(message = "A data de chegada não pode ser futura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataEntrada;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate previsaoEntrega;

    public static VeiculoForm de(Veiculo veiculo) {
        VeiculoForm form = new VeiculoForm();
        form.setMarca(veiculo.getMarca());
        form.setModelo(veiculo.getModelo());
        form.setAno(veiculo.getAno());
        form.setCor(veiculo.getCor());
        form.setPlaca(veiculo.getPlaca());
        form.setQuilometragem(veiculo.getQuilometragem());
        form.setClienteId(veiculo.getCliente().getId());
        form.setDataEntrada(veiculo.getDataEntrada().truncatedTo(ChronoUnit.MINUTES));
        form.setPrevisaoEntrega(veiculo.getPrevisaoEntrega() == null ? null : veiculo.getPrevisaoEntrega().toLocalDate());
        return form;
    }
}
