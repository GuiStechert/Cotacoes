import { signal, dataHora } from './runtime.js';
import { moeda, numero, percentual, mensagem, ROTULOS } from './regras.js';
export class Aprovacao {
    constructor(api, sessao, router) { this.api = api; this.sessao = sessao; this.router = router; }
    dataHora = dataHora;
    rotaId = null;
    moeda = moeda;
    numero = numero;
    percentual = percentual;
    rotulos = ROTULOS;
    cotacao = signal(null);
    produtos = signal([]);
    historico = signal([]);
    erro = signal('');
    ocupado = signal(false);
    confirmando = signal(false);
    estadoEscolhas = signal({});
    get escolhas() { return this.estadoEscolhas(); }
    set escolhas(valor) { this.estadoEscolhas.set(valor); }
    get podeDecidir() { const c = this.cotacao(); return !!c && c.status === 'PENDENTE_APROVACAO' && c.perfilAprovador === this.sessao.usuario()?.perfil && this.sessao.pode(['GESTOR', 'SUPERVISOR', 'GERENTE']) && c.itens.some(i => i.status === 'PENDENTE'); }
    async carregar(id) { this.rotaId = id; this.ocupado.set(true); this.erro.set(''); this.confirmando.set(false); try {
        const [cotacao, produtos, historico] = await Promise.all([this.api.detalhe(id), this.api.produtos(), this.api.historico(id)]);
        this.cotacao.set(cotacao);
        this.produtos.set(produtos);
        this.historico.set(historico);
        this.escolhas = {};
        for (const item of cotacao.itens)
            if (item.status === 'PENDENTE')
                this.escolhas[item.id] = { aprovado: true, comentario: '' };
    }
    catch (e) {
        this.erro.set(mensagem(e));
    }
    finally {
        this.ocupado.set(false);
    } }
    alterar(id, aprovado) { if (this.podeDecidir && !this.ocupado() && this.escolhas[id]) {
        this.escolhas = { ...this.escolhas, [id]: { ...this.escolhas[id], aprovado } };
        this.confirmando.set(false);
    } }
    abrirConfirmacao() { if (this.podeDecidir && !this.ocupado())
        this.confirmando.set(true); }
    async confirmar() {
        if (!this.confirmando() || !this.podeDecidir || this.ocupado())
            return;
        const c = this.cotacao();
        const itens = c.itens.filter(i => i.status === 'PENDENTE').map(i => { const d = this.escolhas[i.id]; return { itemId: i.id, aprovado: d.aprovado, ...(!d.aprovado && d.comentario.trim() ? { comentario: d.comentario.trim() } : {}) }; });
        this.ocupado.set(true);
        this.erro.set('');
        try {
            await this.api.decidir(c.id, itens);
            await this.carregar(c.id);
            this.sessao.aviso.set('Decisões registradas.');
        }
        catch (e) {
            const texto = mensagem(e);
            const codigo = e?.error?.codigo;
            if (codigo === 'CONFLITO_CONCORRENCIA' || codigo === 'ESTADO_INVALIDO')
                await this.carregar(c.id);
            this.erro.set(texto);
        }
        finally {
            this.ocupado.set(false);
            this.confirmando.set(false);
        }
    }
    nome(produto) { return this.produtos().find(p => p.id === produto)?.nome || produto; }
    comentario(id) { return this.historico().find(e => e.itemId === id)?.comentario; }
    get aprovados() { return Object.values(this.escolhas).filter(e => e.aprovado).length; }
    get reprovados() { return Object.values(this.escolhas).filter(e => !e.aprovado).length; }
}
