package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.model.StatusVeiculo;

/**
 * Regras da mudança de status do veículo (classe sem dependências, fácil de testar):
 * - o status anda uma etapa por vez, na ordem: análise, peças, manutenção, teste, concluído, entregue;
 * - qualquer funcionário pode AVANÇAR, porque é ele quem está na manutenção;
 * - só o gerente (e o admin) registra a ENTREGA e pode VOLTAR uma etapa para corrigir um engano.
 */
public final class FluxoStatusVeiculo {

    private FluxoStatusVeiculo() {
    }

    public static void validar(StatusVeiculo atual, StatusVeiculo novo, boolean gerenteOuAdmin) {
        if (novo == null) {
            throw new NegocioException("Selecione o novo status.");
        }
        if (novo == atual) {
            throw new NegocioException("O veículo já está neste status.");
        }
        if ((novo == StatusVeiculo.ENTREGUE || atual == StatusVeiculo.ENTREGUE) && !gerenteOuAdmin) {
            throw new NegocioException("Somente o gerente pode registrar a entrega do veículo.");
        }
        boolean avanco = novo == atual.proximo();
        boolean recuo = novo == atual.anterior();
        if (!avanco && !recuo) {
            throw new NegocioException("O status deve seguir a ordem. Depois de \"" + atual.getRotulo()
                    + "\" o próximo passo é \"" + (atual.proximo() == null ? "—" : atual.proximo().getRotulo()) + "\".");
        }
        if (recuo && !gerenteOuAdmin) {
            throw new NegocioException("Somente o gerente pode voltar o status do veículo.");
        }
    }

    /** Etapa para a qual este usuário pode avançar agora, ou null se não puder. */
    public static StatusVeiculo proximoPara(StatusVeiculo atual, boolean gerenteOuAdmin) {
        StatusVeiculo proximo = atual.proximo();
        if (proximo == StatusVeiculo.ENTREGUE && !gerenteOuAdmin) {
            return null;
        }
        return proximo;
    }

    /** Voltar uma etapa é só do gerente e do admin. */
    public static StatusVeiculo anteriorPara(StatusVeiculo atual, boolean gerenteOuAdmin) {
        return gerenteOuAdmin ? atual.anterior() : null;
    }
}
