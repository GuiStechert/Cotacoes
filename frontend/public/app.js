import { dataHora } from './runtime.js';
import { ROTULOS } from './regras.js';
export class App {
    constructor(api, sessao, router) { this.api = api; this.sessao = sessao; this.router = router; }
    dataHora = dataHora;
    rotulos = ROTULOS;
    get iniciais() { return (this.sessao.usuario()?.nome || '').split(/\s+/).filter(Boolean).slice(0, 2).map(n => n[0]).join('').toUpperCase(); }
    async tentarNovamente() { if (await this.sessao.iniciar(true))
        await this.router.navigate(['/cotacoes']); }
}
