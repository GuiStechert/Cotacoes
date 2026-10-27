export class Api {
 constructor(fetcher=globalThis.fetch.bind(globalThis),token=()=>null){this.fetcher=fetcher;this.token=token;}
 async request(url,method='GET',body){
  if(typeof url!=='string'||!url.startsWith('/api/'))throw new Error('URL fora da API');
  const headers={};if(body!==undefined)headers['Content-Type']='application/json';
  const token=this.token();if(method!=='GET'&&token)headers[token.headerName]=token.token;
  const response=await this.fetcher(url,{method,credentials:'same-origin',headers,...(body===undefined?{}:{body:JSON.stringify(body)})});
  if(!response.ok){let error;try{error=await response.json();}catch{error={};}throw {status:response.status,error};}
  return response.status===204?undefined:response.json();
 }
 usuario(){return this.request('/api/usuario');}
 csrf(){return this.request('/api/csrf');}
 clientes(){return this.request('/api/clientes');}
 produtos(){return this.request('/api/produtos');}
 listar(filtro,fila=false){const params=new URLSearchParams({page:String(filtro.page),size:'20'});if(!fila)for(const key of ['status','clienteId','inicio','fim'])if(filtro[key])params.set(key,filtro[key]);return this.request((fila?'/api/aprovacoes/pendentes':'/api/cotacoes')+'?'+params);}
 url(id){return '/api/cotacoes/'+encodeURIComponent(id);}
 detalhe(id){return this.request(this.url(id));}
 historico(id){return this.request(this.url(id)+'/historico');}
 criar(body){return this.request('/api/cotacoes','POST',body);}
 editar(id,body){return this.request(this.url(id),'PUT',body);}
 enviar(id){return this.request(this.url(id)+'/enviar','POST',{});}
 duplicar(id){return this.request(this.url(id)+'/duplicar','POST',{});}
 decidir(id,itens){return this.request(this.url(id)+'/decisoes','POST',{itens});}
}
