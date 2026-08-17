import {SUPABASE_URL,SUPABASE_KEY} from './config.js';import{getSession,setSession,clearSession}from'./db.js';
async function call(path,body,token=''){const h={apikey:SUPABASE_KEY,'Content-Type':'application/json'};if(token)h.Authorization=`Bearer ${token}`;const r=await fetch(`${SUPABASE_URL}/auth/v1/${path}`,{method:'POST',headers:h,body:body?JSON.stringify(body):undefined});const d=await r.json().catch(()=>({}));if(!r.ok)throw new Error(d.msg||d.message||d.error_description||d.error||`Error ${r.status}`);return d}
function pack(d){if(!d.access_token)return null;return{accessToken:d.access_token,refreshToken:d.refresh_token,expiresAt:Date.now()+(Math.max(60,+d.expires_in||3600)-60)*1000,user:d.user}}
export async function signIn(email,password){const s=pack(await call('token?grant_type=password',{email,password}));await setSession(s);return s}
export async function signUp(email,password){const d=await call('signup',{email,password});const s=pack(d);if(s)await setSession(s);return{s,user:d.user}}
export async function currentSession(){let s=await getSession();if(!s)return null;if(s.expiresAt>Date.now())return s;try{const n=pack(await call('token?grant_type=refresh_token',{refresh_token:s.refreshToken}));await setSession(n);return n}catch{await clearSession();return null}}
export async function signOut(){await clearSession()}
