import { signal } from './runtime.js';
import { mensagem } from './regras.js';
export class Sessao {
    api;
    usuario = signal(null);
    token = signal(null);
    erro = signal('');
    carregando = signal(false);
    aviso = signal('');
    constructor(api) {
        this.api = api;
    }
    tarefa = null;
    iniciada = false;
    iniciar(force = false) {
        if (this.tarefa)
            return this.tarefa;
        if (this.iniciada && !force)
            return Promise.resolve(this.usuario());
        this.carregando.set(true);
        this.erro.set('');
        this.usuario.set(null);
        this.token.set(null);
        this.tarefa = Promise.resolve().then(async () => {
            const usuario = await this.api.usuario();
            if (!usuario?.id || !usuario.nome || !['COMERCIAL', 'GESTOR', 'SUPERVISOR', 'GERENTE'].includes(usuario.perfil))
                throw { error: { codigo: 'ACESSO_NEGADO' } };
            const token = await this.api.csrf();
            if (!token?.token || token.headerName !== 'X-CSRF-TOKEN')
                throw new Error();
            this.token.set(token);
            this.usuario.set(usuario);
            return usuario;
        }).catch(erro => { this.erro.set(mensagem(erro)); return null; }).finally(() => { this.carregando.set(false); this.iniciada = true; this.tarefa = null; });
        return this.tarefa;
    }
    pode(perfis) { const u = this.usuario(); return !!u && perfis.includes(u.perfil); }
}
