import {LuauState} from './index.js';
export async function validate(source){
  if(typeof source!=='string'||source.length>1000000)return {valid:false,reason:'size'};
  let state;
  try{state=await LuauState.createAsync();state.loadstring(source,'script',true);return {valid:true};}
  catch(error){if(error.name!=='CompileError')return {valid:false,reason:'unavailable'};const diagnostic=String(error.message),match=diagnostic.match(/:(\d+):/);return {valid:false,diagnostic,line:match?Number(match[1]):0};}
  finally{if(state)state.destroy();}
}
