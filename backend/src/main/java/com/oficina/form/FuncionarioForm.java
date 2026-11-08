package com.oficina.form;

import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FuncionarioForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o CPF.")
    @Pattern(regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$", message = "CPF inválido. Use 000.000.000-00.")
    private String cpf;

    @Size(max = 20, message = "Use no máximo 20 caracteres.")
    @Pattern(regexp = "^[0-9()+\\s-]*$", message = "Telefone inválido.")
    private String telefone;

    @Email(message = "E-mail inválido.")
    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String email;

    @Size(max = 60, message = "Use no máximo 60 caracteres.")
    private String cargo;

    /** Só o administrador vê e altera. Para os outros perfis o campo nem aparece e o serviço o ignora. */
    @DecimalMin(value = "0.00", message = "O salário não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Use até 8 dígitos e 2 casas decimais.")
    private BigDecimal salario;

    @NotNull(message = "Informe a situação.")
    private StatusFuncionario status = StatusFuncionario.ATIVO;

    public static FuncionarioForm de(Funcionario funcionario) {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome(funcionario.getNome());
        form.setCpf(funcionario.getCpf());
        form.setTelefone(funcionario.getTelefone());
        form.setEmail(funcionario.getEmail());
        form.setCargo(funcionario.getCargo());
        form.setSalario(funcionario.getSalario());
        form.setStatus(funcionario.getStatus());
        return form;
    }
}
