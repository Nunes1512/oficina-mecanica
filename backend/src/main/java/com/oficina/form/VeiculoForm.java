package com.oficina.form;

import com.oficina.model.StatusVeiculo;
import com.oficina.model.Veiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate previsaoEntrega;

    private StatusVeiculo status;

    public static VeiculoForm de(Veiculo veiculo) {
        VeiculoForm form = new VeiculoForm();
        form.setMarca(veiculo.getMarca());
        form.setModelo(veiculo.getModelo());
        form.setAno(veiculo.getAno());
        form.setCor(veiculo.getCor());
        form.setPlaca(veiculo.getPlaca());
        form.setQuilometragem(veiculo.getQuilometragem());
        form.setClienteId(veiculo.getCliente().getId());
        form.setPrevisaoEntrega(veiculo.getPrevisaoEntrega() == null ? null : veiculo.getPrevisaoEntrega().toLocalDate());
        form.setStatus(veiculo.getStatus());
        return form;
    }
}
