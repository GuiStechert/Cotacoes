import {Component,inject,OnInit} from '@angular/core';
import {Router,RouterLink,RouterLinkActive,RouterOutlet} from '@angular/router';
import {Sessao} from './sessao';import {ROTULOS} from './regras';
@Component({selector:'app-root',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app.html'})
export class App implements OnInit {
 readonly sessao=inject(Sessao);private router=inject(Router);rotulos=ROTULOS;
 ngOnInit(){void this.sessao.iniciar();}
 get iniciais(){return (this.sessao.usuario()?.nome||'').split(/\s+/).filter(Boolean).slice(0,2).map(n=>n[0]).join('').toUpperCase();}
 async tentarNovamente(){if(await this.sessao.iniciar(true))await this.router.navigate(['/cotacoes']);}
}
