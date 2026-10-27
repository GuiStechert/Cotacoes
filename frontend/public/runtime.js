export function signal(value) {
 const state=globalThis.Vue.ref(value);
 const read=()=>state.value;
 read.set=value=>{state.value=value;};
 return read;
}
export function dataHora(value){return value?new Intl.DateTimeFormat('pt-BR',{dateStyle:'short',timeStyle:'short',timeZone:'America/Cuiaba'}).format(new Date(value)):'—';}
export function link(parts){return Array.isArray(parts)?parts.map((part,index)=>index?encodeURIComponent(part):part).join('/'):parts;}
export function controlador(instance){
 const state=globalThis.Vue.reactive(instance);
 for(let proto=Object.getPrototypeOf(instance);proto&&proto!==Object.prototype;proto=Object.getPrototypeOf(proto))for(const key of Object.getOwnPropertyNames(proto)){
  const descriptor=Object.getOwnPropertyDescriptor(proto,key);
  if(key!=='constructor'&&typeof descriptor.value==='function')state[key]=state[key].bind(state);
  if(descriptor.get)Object.defineProperty(instance,key,{enumerable:true,configurable:true,get:()=>descriptor.get.call(state),...(descriptor.set?{set:value=>descriptor.set.call(state,value)}:{})});
 }
 state.link=link;
 return state;
}
