const DB='taskium-v2', VER=1;
export function openDb(){return new Promise((res,rej)=>{const r=indexedDB.open(DB,VER);r.onupgradeneeded=()=>{const d=r.result;if(!d.objectStoreNames.contains('records')){const s=d.createObjectStore('records',{keyPath:'id'});s.createIndex('type','type');}if(!d.objectStoreNames.contains('settings'))d.createObjectStore('settings',{keyPath:'key'});if(!d.objectStoreNames.contains('auth'))d.createObjectStore('auth',{keyPath:'key'});};r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error);});}
function req(r){return new Promise((res,rej)=>{r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error);});}
async function tx(store,mode,fn){const d=await openDb(),t=d.transaction(store,mode),s=t.objectStore(store),out=fn(s);if(out instanceof IDBRequest)return req(out);return new Promise((res,rej)=>{t.oncomplete=()=>res(out);t.onerror=()=>rej(t.error);});}
export async function all(includeDeleted=false){const d=await openDb(),t=d.transaction('records','readonly'),rows=await req(t.objectStore('records').getAll());return includeDeleted?rows:rows.filter(x=>!x._sync?.deleted);}
export const put=r=>tx('records','readwrite',s=>s.put(r));
export async function putMany(rows){for(const r of rows)await put(r)}
export const remove=id=>tx('records','readwrite',s=>s.delete(id));
export const setting=async(k,f=null)=>(await tx('settings','readonly',s=>s.get(k)))?.value??f;
export const setSetting=(k,v)=>tx('settings','readwrite',s=>s.put({key:k,value:v}));
export const getSession=async()=> (await tx('auth','readonly',s=>s.get('session')))?.value||null;
export const setSession=v=>tx('auth','readwrite',s=>s.put({key:'session',value:v}));
export const clearSession=()=>tx('auth','readwrite',s=>s.delete('session'));
