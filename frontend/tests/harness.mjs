import {describe,it as test,beforeEach,afterEach} from 'node:test';
import * as chai from 'chai';
import {JestChaiExpect,JestAsymmetricMatchers} from '@vitest/expect';
import {fn,spyOn} from '@vitest/spy';
import {JSDOM} from 'jsdom';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import {controlador} from '../public/runtime.js';
const dom=new JSDOM('<!doctype html><body></body>',{url:'http://localhost',runScripts:'outside-only'});
export const window=dom.window;
dom.getInternalVMContext().console=console;
vm.runInContext(readFileSync(new URL('./vue.global.prod.cjs',import.meta.url),'utf8'),dom.getInternalVMContext());
globalThis.Vue=dom.window.Vue;
chai.use(JestChaiExpect);chai.use(JestAsymmetricMatchers);
export const expect=chai.expect,vi={fn,spyOn};
export {describe,beforeEach,afterEach};
export const it=Object.assign(test,{each:values=>(name,callback)=>values.forEach(value=>test(name.replace('%s',String(value)),()=>callback(...(Array.isArray(value)?value:[value]))))});
export const signal=value=>{const state=Vue.ref(value);const get=()=>state.value;get.set=value=>state.value=value;return get;};
export const Router=class {},provideRouter=()=>null;
let providers=[],mounted=[];
afterEach(()=>{for(const app of mounted)app.unmount();mounted=[];dom.window.document.body.innerHTML='';});
export const TestBed={
 configureTestingModule(config){providers=config.providers;this.router={navigate:vi.fn().mockResolvedValue(true)};},
 inject(type){return type===Router?this.router:providers.find(p=>p?.provide===type)?.useValue;},
 createComponent(Type){
  const api=providers.find(p=>p?.provide?.name==='Api')?.useValue;
  const sessao=providers.find(p=>p?.provide?.name==='Sessao')?.useValue;
  const instance=controlador(new Type(api,sessao,this.router));
  const host=dom.window.document.createElement('div');dom.window.document.body.append(host);
  const template=readFileSync(new URL(`../public/${Type.name.toLowerCase()}.html`,import.meta.url),'utf8');
  const app=Vue.createApp({setup:()=>instance,template});
  const errors=[];app.config.errorHandler=e=>{errors.push(e);};const root=app.mount(host);mounted.push(app);
  return {componentInstance:instance,nativeElement:host,detectChanges(){root.$.update();if(errors.length)throw errors.shift();},async whenStable(){await Vue.nextTick();root.$.update();if(errors.length)throw errors.shift();}};
 }
};
