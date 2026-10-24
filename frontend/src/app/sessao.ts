import {Injectable,signal} from '@angular/core';
import {Api} from './api';
import {Perfil,Usuario,TokenCsrf} from './models';
import {mensagem} from './regras';
@Injectable({providedIn:'root'})
export class Sessao {
 readonly usuario=signal<Usuario|null>(null);readonly token=signal<TokenCsrf|null>(null);
 readonly erro=signal('');readonly carregando=signal(false);readonly aviso=signal('');
 constructor(private api:Api) {}
 private tarefa:Promise<Usuario|null>|null=null;private iniciada=false;
 iniciar(force=false):Promise<Usuario|null>{
  if(this.tarefa)return this.tarefa;if(this.iniciada&&!force)return Promise.resolve(this.usuario());
  this.carregando.set(true);this.erro.set('');this.usuario.set(null);this.token.set(null);
  this.tarefa=Promise.resolve().then(async()=>{
   const usuario=await this.api.usuario();
   if(!usuario?.id||!usuario.nome||!['COMERCIAL','GESTOR','SUPERVISOR','GERENTE'].includes(usuario.perfil))throw {error:{codigo:'ACESSO_NEGADO'}};
   const token=await this.api.csrf();if(!token?.token||token.headerName!=='X-CSRF-TOKEN')throw new Error();
   this.token.set(token);this.usuario.set(usuario);return usuario;
  }).catch(erro=>{this.erro.set(mensagem(erro));return null;}).finally(()=>{this.carregando.set(false);this.iniciada=true;this.tarefa=null;});
  return this.tarefa;
 }
 pode(perfis:Perfil[]):boolean {const u=this.usuario();return !!u&&perfis.includes(u.perfil);}
}
