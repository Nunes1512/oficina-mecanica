"use strict";
document.addEventListener('DOMContentLoaded', () => {
    const cliente = document.getElementById('clienteId');
    const placa = document.getElementById('placa');
    if (cliente && placa) {
        const veiculos = [...document.querySelectorAll('.os-veiculo-data')].map(el => el.dataset);
        const setText = (id, value) => { const el = document.getElementById(id); if (el) el.textContent = value || '—'; };
        const normalizar = s => (s || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
        const atualizar = () => {
            const selected = cliente.selectedOptions[0];
            setText('clienteCpf', selected?.dataset.cpf);
            setText('clienteTelefone', selected?.dataset.telefone);
            setText('clienteEmail', selected?.dataset.email);
            const lista = document.getElementById('placasDoCliente');
            lista.replaceChildren();
            const disponiveis = veiculos.filter(v => v.cliente === cliente.value);
            disponiveis.forEach(v => {
                const op = document.createElement('option');
                op.value = v.placa;
                op.label = `${v.marca} ${v.modelo}`;
                lista.append(op);
            });
            const veiculo = disponiveis.find(v => normalizar(v.placa) === normalizar(placa.value));
            setText('veiculoModelo', veiculo ? `${veiculo.marca} ${veiculo.modelo}` : '—');
            setText('veiculoAnoCor', veiculo ? `${veiculo.ano || '—'} / ${veiculo.cor || '—'}` : '—');
            setText('veiculoKm', veiculo?.km);
            setText('veiculoProprietario', veiculo?.proprietario);
            placa.setCustomValidity(placa.value && !veiculo ? 'Selecione uma placa do cliente escolhido.' : '');
        };
        cliente.addEventListener('change', () => { placa.value = ''; atualizar(); });
        placa.addEventListener('input', atualizar);
        atualizar();
    }
    const btn = document.getElementById('adicionarItem');
    const tbody = document.getElementById('itensAvaliacao');
    if (btn && tbody) {
        const rowModelo = tbody.rows[0]?.cloneNode(true);
        const prepararLinha = linha => {
            const tipo = linha.querySelector('[name="tipos"]');
            const peca = linha.querySelector('[name="pecasIds"]');
            if (tipo && peca) {
                const ePeca = tipo.value === 'PECA';
                // Não desabilitar: o valor vazio precisa ser enviado para manter alinhadas as listas do formulário.
                peca.required = ePeca;
                peca.classList.toggle('os-nao-aplica', !ePeca);
                peca.tabIndex = ePeca ? 0 : -1;
                if (!ePeca) peca.value = '';
            }
        };
        tbody.querySelectorAll('tr').forEach(prepararLinha);
        btn.addEventListener('click', () => {
            if (!rowModelo || tbody.rows.length >= 100) return;
            const nova = rowModelo.cloneNode(true);
            nova.querySelectorAll('input').forEach(el => { el.value = el.type === 'number' ? '1' : ''; });
            nova.querySelectorAll('select').forEach(el => { el.selectedIndex = 0; });
            tbody.append(nova);
            prepararLinha(nova);
            nova.querySelector('[name="tipos"]')?.focus();
        });
        tbody.addEventListener('click', event => {
            if (!event.target.closest('.os-remove-item')) return;
            const linha = event.target.closest('tr');
            linha?.remove();
            // Para evitar um formulário sem campos, preserve uma linha vazia.
            if (tbody.rows.length === 0) btn.click();
        });
        tbody.addEventListener('change', event => {
            if (event.target.matches('[name="tipos"]')) prepararLinha(event.target.closest('tr'));
        });
    }

    const adminCliente = document.getElementById('adminClienteId');
    const adminPlaca = document.getElementById('adminPlaca');
    if (adminCliente && adminPlaca) {
        const opcoes = [...adminPlaca.options];
        const atualizarPlacas = () => {
            opcoes.forEach(op => { op.hidden = op.dataset.cliente !== adminCliente.value; });
            const atual = adminPlaca.selectedOptions[0];
            if (!atual || atual.hidden) {
                adminPlaca.value = opcoes.find(op => !op.hidden)?.value || '';
            }
        };
        adminCliente.addEventListener('change', atualizarPlacas);
        atualizarPlacas();
    }
});
