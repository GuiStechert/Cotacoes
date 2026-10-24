import {HttpInterceptorFn} from '@angular/common/http';
import {CanActivateFn} from '@angular/router';
import {Perfil} from './models';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {Sessao} from './sessao';
export const csrfInterceptor:HttpInterceptorFn=(req,next)=>{
 if(!req.url.startsWith('/api/'))return next(req);
 const token=inject(Sessao).token();let copia=req.clone({withCredentials:true});
 if(token&&!['GET','HEAD','OPTIONS'].includes(req.method))copia=copia.clone({setHeaders:{[token.headerName]:token.token}});
 return next(copia);
};
export function perfilGuard(perfis:Perfil[]):CanActivateFn {return async()=>{const sessao=inject(Sessao),router=inject(Router);const usuario=await sessao.iniciar();if(!usuario)return false;return perfis.includes(usuario.perfil)||router.createUrlTree(['/cotacoes']);};}
