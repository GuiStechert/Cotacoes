import { TestBed, provideRouter, Router, signal } from './harness.mjs';
import { describe, it, expect, vi } from './harness.mjs';
import { App } from '../public/app.js';
import { Sessao } from '../public/sessao.js';
import { ROUTES } from '../public/routes.js';
describe('Moldura e rotas', () => {
    function montar(user) { const usuario = signal(user); const sessao = { usuario, token: signal(null), carregando: signal(false), erro: signal(user ? '' : 'Você não tem permissão para esta ação.'), aviso: signal(''), pode: (perfis) => !!usuario() && perfis.includes(usuario().perfil), iniciar: vi.fn().mockResolvedValue(user) }; TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Sessao, useValue: sessao }] }); const f = TestBed.createComponent(App); f.detectChanges(); return { f, sessao }; }
    it('possui exatamente três telas e redirecionamentos sem login adicional', () => { const telas = ROUTES.filter(r => r.component); expect(telas.map(r => r.path)).toEqual(['cotacoes', 'cotacao/:id', 'aprovacao/:id']); expect(new Set(telas.map(r => r.component)).size).toBe(3); expect(telas.every(r => r.canActivate?.length)).toBe(true); });
    it('mostra nome, perfil e navegação Comercial', () => { const { f } = montar({ id: 'u', nome: 'Ana Silva', perfil: 'COMERCIAL' }); expect(f.nativeElement.textContent).toContain('Ana Silva'); expect(f.nativeElement.textContent).toContain('Comercial'); expect(f.nativeElement.querySelector('a[href="/cotacao/nova"]')).not.toBeNull(); expect(f.nativeElement.querySelector('.avatar').textContent.trim()).toBe('AS'); });
    it('aprovador não recebe atalho para criar cotação', () => { const { f } = montar({ id: 'g', nome: 'Gerente', perfil: 'GERENTE' }); expect(f.nativeElement.querySelector('a[href="/cotacao/nova"]')).toBeNull(); expect(f.nativeElement.textContent).toContain('Gerente'); });
    it('erro de autenticação não abre telas nem oferece escolha de perfil', () => { const { f } = montar(null); expect(f.nativeElement.textContent).toContain('Você não tem permissão'); expect(f.nativeElement.querySelector('router-outlet')).toBeNull(); expect(f.nativeElement.querySelector('select')).toBeNull(); });
    it('retry explícito tenta autenticar de novo', async () => { const { f, sessao } = montar(null); const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true); sessao.iniciar.mockResolvedValue({ id: 'u', nome: 'Ana', perfil: 'COMERCIAL' }); await f.componentInstance.tentarNovamente(); expect(sessao.iniciar).toHaveBeenCalledWith(true); expect(navegar).toHaveBeenCalledWith(['/cotacoes']); });
});
