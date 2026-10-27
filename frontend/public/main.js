import {iniciarAplicacao} from './bootstrap.js';
if(globalThis.Vue){
 iniciarAplicacao().catch(()=>{document.querySelector('#app').textContent='Não foi possível carregar a interface. Atualize a página para tentar novamente.';});
}else document.querySelector('#app').textContent='Não foi possível carregar o Vue do CDN. Verifique a conexão com cdn.jsdelivr.net e atualize a página.';
