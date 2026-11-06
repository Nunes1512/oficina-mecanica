// Pede confirmação antes de enviar formulários marcados com data-confirm.
// Fica em arquivo externo porque a política de segurança (CSP) bloqueia scripts dentro da página.
document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    if (formulario.dataset && formulario.dataset.confirm && !window.confirm(formulario.dataset.confirm)) {
        evento.preventDefault();
    }
});
