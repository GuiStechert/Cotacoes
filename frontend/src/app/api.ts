import {Injectable} from '@angular/core';
import {HttpClient,HttpParams} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {Cliente,Produto,Usuario,TokenCsrf,Filtro,Pagina,Detalhe,Entrada,Evento,Decisao} from './models';
@Injectable({providedIn:'root'})
export class Api {
 constructor(private http:HttpClient) {}
 usuario():Promise<Usuario>{return firstValueFrom(this.http.get<Usuario>('/api/usuario'));}
 csrf():Promise<TokenCsrf>{return firstValueFrom(this.http.get<TokenCsrf>('/api/csrf'));}
 clientes():Promise<Cliente[]>{return firstValueFrom(this.http.get<Cliente[]>('/api/clientes'));}
 produtos():Promise<Produto[]>{return firstValueFrom(this.http.get<Produto[]>('/api/produtos'));}
 listar(filtro:Filtro,fila=false):Promise<Pagina>{
  let params=new HttpParams().set('page',filtro.page).set('size',20);
  if(!fila)for(const chave of ['status','clienteId','inicio','fim']as const)if(filtro[chave])params=params.set(chave,filtro[chave]!);
  return firstValueFrom(this.http.get<Pagina>(fila?'/api/aprovacoes/pendentes':'/api/cotacoes',{params}));
 }
 detalhe(id:string):Promise<Detalhe>{return firstValueFrom(this.http.get<Detalhe>(this.url(id)));}
 historico(id:string):Promise<Evento[]>{return firstValueFrom(this.http.get<Evento[]>(this.url(id)+'/historico'));}
 criar(entrada:Entrada):Promise<{id:string}>{return firstValueFrom(this.http.post<{id:string}>('/api/cotacoes',entrada));}
 editar(id:string,entrada:Entrada):Promise<void>{return firstValueFrom(this.http.put<void>(this.url(id),entrada));}
 enviar(id:string):Promise<void>{return firstValueFrom(this.http.post<void>(this.url(id)+'/enviar',{}));}
 duplicar(id:string):Promise<{id:string}>{return firstValueFrom(this.http.post<{id:string}>(this.url(id)+'/duplicar',{}));}
 decidir(id:string,itens:Decisao[]):Promise<void>{return firstValueFrom(this.http.post<void>(this.url(id)+'/decisoes',{itens}));}
 private url(id:string){return '/api/cotacoes/'+encodeURIComponent(id);}
}
