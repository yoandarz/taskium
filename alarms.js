import { VAPID_PUBLIC_KEY } from './constants.js';
import { currentSession } from './auth.js';
import { savePushSubscription, deletePushSubscription } from './cloud.js';
import { getSettings, pendingSummary } from './task-service.js';
import { localDateIso } from './utils.js';
function urlBase64ToUint8Array(base64String) { const padding = '='.repeat((4 - base64String.length % 4) % 4), base64 = (base64String + padding).replace(/-/g,'+').replace(/_/g,'/'), raw = atob(base64); return Uint8Array.from([...raw].map(c => c.charCodeAt(0))); }
export function notificationSupport() { return { supported: 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window, permission: 'Notification' in window ? Notification.permission : 'unsupported' }; }
export async function enablePush() {
  const support = notificationSupport(); if (!support.supported) throw new Error('Este navegador no admite notificaciones push para esta PWA.');
  const session = await currentSession(); if (!session?.user?.id) throw new Error('Inicia sesión antes de activar las alertas en este dispositivo.');
  const permission = await Notification.requestPermission(); if (permission !== 'granted') throw new Error('El permiso de notificaciones no fue concedido.');
  const registration = await navigator.serviceWorker.ready; let subscription = await registration.pushManager.getSubscription();
  if (!subscription) subscription = await registration.pushManager.subscribe({ userVisibleOnly:true, applicationServerKey:urlBase64ToUint8Array(VAPID_PUBLIC_KEY) });
  await savePushSubscription(subscription, session.user.id); return subscription;
}
export async function disablePush() { const registration = await navigator.serviceWorker.ready, subscription = await registration.pushManager.getSubscription(); if (!subscription) return; try { await deletePushSubscription(subscription.endpoint); } catch {} await subscription.unsubscribe(); }
export async function pushIsEnabled() { if (!notificationSupport().supported) return false; const registration = await navigator.serviceWorker.ready; return Boolean(await registration.pushManager.getSubscription()); }
function minuteParts(date = new Date()) { return { date:localDateIso(date), hour:date.getHours(), minute:date.getMinutes() }; }
function firedKeys() { try { return new Set(JSON.parse(localStorage.getItem('taskium-fired-keys') || '[]')); } catch { return new Set(); } }
function rememberFired(key) { const keys = [...firedKeys(), key].slice(-80); localStorage.setItem('taskium-fired-keys', JSON.stringify(keys)); }
export async function checkLocalAlarm() {
  const summary = await pendingSummary(); if (summary.totalDebtUnits <= 0) return null; const settings = await getSettings(), now = new Date(), parts = minuteParts(now), fired = firedKeys();
  if (settings.snoozeUntil) { const due = new Date(settings.snoozeUntil); if (due.getTime() <= now.getTime() && due.getTime() > now.getTime() - 60000) { const key = `snooze:${settings.snoozeUntil}`; if (!fired.has(key)) { rememberFired(key); return { key, kind:'snooze' }; } } }
  const jsDay = now.getDay(), dayIndex = jsDay === 0 ? 6 : jsDay - 1, day = settings.alarmsByDay?.[dayIndex];
  if (!day?.slots) return null;
  for (let i=0;i<day.slots.length;i+=1) { const slot = day.slots[i]; if (!slot.enabled || Number(slot.hour)!==parts.hour || Number(slot.minute)!==parts.minute) continue; const key=`alarm:${parts.date}:${parts.hour}:${parts.minute}:${i}`; if (!fired.has(key)) { rememberFired(key); return { key, kind:'alarm' }; } }
  return null;
}
export function startAlarmMonitor(onFire) { let stopped=false; const tick=async()=>{ if(stopped)return; try { const due=await checkLocalAlarm(); if(due) onFire?.(due); } catch(e){ console.warn('Alarma local',e); } }; tick(); const id=setInterval(tick,20000); return ()=>{stopped=true;clearInterval(id);}; }
