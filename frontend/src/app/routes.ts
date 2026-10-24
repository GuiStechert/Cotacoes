import {Routes} from '@angular/router';
import {Lista} from './lista';import {Cotacao} from './cotacao';import {Aprovacao} from './aprovacao';import {perfilGuard} from './acesso';
export const ROUTES:Routes=[
 {path:'',redirectTo:'cotacoes',pathMatch:'full'},
 {path:'cotacoes',component:Lista,canActivate:[perfilGuard(['COMERCIAL','GESTOR','SUPERVISOR','GERENTE'])]},
 {path:'cotacao/:id',component:Cotacao,canActivate:[perfilGuard(['COMERCIAL'])]},
 {path:'aprovacao/:id',component:Aprovacao,canActivate:[perfilGuard(['GESTOR','SUPERVISOR','GERENTE'])]},
 {path:'**',redirectTo:'cotacoes'}
];
