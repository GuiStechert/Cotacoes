import { signal, dataHora } from './runtime.js';
import { moeda, mensagem, ROTULOS } from './regras.js';
export class Lista {
    constructor(api, sessao, router) { this.api = api; this.sessao = sessao; this.router = router; }
    dataHora = dataHora;
    inicializada = false;
    moeda = moeda;
    rotulos = ROTULOS;
    pagina = signal({ pagina: 0, tamanho: 20, total: 0, totalPaginas: 0, conteudo: [] });
    clientes = signal([]);
    erro = signal('');
    carregando = signal(false);
    fila = signal(false);
    duplicando = signal(null);
    filtro = { page: 0 };
    async carregar() {
        this.inicializada = true;
        this.carregando.set(true);
        this.erro.set('');
        try {
            const [pagina, clientes] = await Promise.all([this.api.listar({ ...this.filtro }, this.fila()), this.api.clientes()]);
            this.pagina.set(pagina);
            this.clientes.set(clientes);
        }
        catch (e) {
            this.erro.set(mensagem(e));
        }
        finally {
            this.carregando.set(false);
        }
    }
    async filtrar() { if (this.filtro.inicio && this.filtro.fim && this.filtro.fim < this.filtro.inicio) {
        this.erro.set('Confira o período informado.');
        return;
    } this.filtro.page = 0; await this.carregar(); }
    async anterior() { if (this.carregando() || this.pagina().pagina <= 0)
        return; this.filtro.page = this.pagina().pagina - 1; await this.carregar(); }
    async proxima() { if (this.carregando() || this.pagina().pagina + 1 >= this.pagina().totalPaginas)
        return; this.filtro.page = this.pagina().pagina + 1; await this.carregar(); }
    async duplicar(id) {
        const r = this.pagina().conteudo.find(r => r.id === id);
        if (this.duplicando() || !r?.podeDuplicar || r.status === 'RASCUNHO' || !this.sessao.pode(['COMERCIAL']))
            return;
        this.duplicando.set(id);
        this.erro.set('');
        try {
            const copia = await this.api.duplicar(id);
            this.sessao.aviso.set('Cópia criada como rascunho.');
            await this.router.navigate(['/cotacao', copia.id]);
        }
        catch (e) {
            this.erro.set(mensagem(e));
        }
        finally {
            this.duplicando.set(null);
        }
    }
    get inicio() { return this.pagina().total ? this.pagina().pagina * 20 + 1 : 0; }
    get fim() { return Math.min((this.pagina().pagina + 1) * 20, this.pagina().total); }
}
