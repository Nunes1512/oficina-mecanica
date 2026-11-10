package com.oficina.form;

import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Um só formulário para cadastro de funcionário + criação automática do login. */
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

    @NotBlank(message = "Informe o e-mail: ele será usado como login.")
    @Email(message = "E-mail inválido.")
    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String email;

    @Size(max = 60, message = "Use no máximo 60 caracteres.")
    private String cargo;

    @NotNull(message = "Informe a situação.")
    private StatusFuncionario status = StatusFuncionario.ATIVO;

    /** Obrigatório na criação; na edição, apenas para ativar conta de cadastro legado. */
    @Size(max = 64, message = "A senha deve ter no máximo 64 caracteres.")
    private String senhaInicial;
    private String perfilUsuario;

    public static FuncionarioForm de(Funcionario funcionario) {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome(funcionario.getNome());
        form.setCpf(funcionario.getCpf());
        form.setTelefone(funcionario.getTelefone());
        form.setEmail(funcionario.getEmail());
        form.setCargo(funcionario.getCargo());
        form.setStatus(funcionario.getStatus());
        return form;
    }
}
