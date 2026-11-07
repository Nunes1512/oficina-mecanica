package com.oficina.form;

import com.oficina.model.Cliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteForm {

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

    @Size(max = 255, message = "Use no máximo 255 caracteres.")
    private String endereco;

    public static ClienteForm de(Cliente cliente) {
        ClienteForm form = new ClienteForm();
        form.setNome(cliente.getNome());
        form.setCpf(cliente.getCpf());
        form.setTelefone(cliente.getTelefone());
        form.setEmail(cliente.getEmail());
        form.setEndereco(cliente.getEndereco());
        return form;
    }
}
