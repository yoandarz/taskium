import { getAllRecords, getRecord, putRecord } from './db.js';
import { defaultSettingsPayload, normalizeRecord, normalizeSettingsRecord, normalizeTask } from './schema.js';
import { markRecordDeleted, markRecordDirty } from './sync.js';
import { daysBetween, localDateIso, nowIso, recurrenceState, uuid } from './utils.js';
export async function ensureDefaults() {
  let settings = await getRecord('settings:main');
  if (!settings) { settings = normalizeSettingsRecord({ ...defaultSettingsPayload(), _sync: { dirty:true, seed:true } }); await putRecord(settings); }
  return settings;
}
export async function getSettings() { return (await getRecord('settings:main')) || ensureDefaults(); }
export async function updateSettings(patch) { const settings = await getSettings(); Object.assign(settings, patch); return markRecordDirty(settings); }
export async function getTasks() { const tasks = (await getAllRecords({ type:'task' })).map(normalizeTask); return tasks.sort((a,b) => Number(a.order)-Number(b.order) || a.createdAt.localeCompare(b.createdAt)); }
export async function addTask(text, taskKind = 'one_time', intervalDays = 1) {
  const clean = String(text || '').trim(); if (!clean) throw new Error('Escribe una tarea.'); const now = nowIso();
  const task = normalizeTask({ id:uuid(), taskKind, text:clean, intervalDays, anchorDate:localDateIso(), completedCount:0, order:Date.now(), createdAt:now, updatedAt:now, _sync:{dirty:true,seed:false} }); await putRecord(task); return task;
}
export async function completeOneTime(id) { const task = await getRecord(id); if (!task || task.taskKind !== 'one_time') return false; await markRecordDeleted(task); return true; }
export async function deleteTask(id) { const task = await getRecord(id); if (!task) return false; await markRecordDeleted(task); return true; }
export async function registerProgress(id) { const task = await getRecord(id); if (!task || task.taskKind !== 'recurring') return false; const state = recurrenceState(task); if (state.debt <= 0) return false; task.completedCount = state.completedCount + 1; await markRecordDirty(task); return true; }
export async function moveTask(id, direction) {
  const task = await getRecord(id); if (!task) return false; const same = (await getTasks()).filter(item => item.taskKind === task.taskKind); const i = same.findIndex(item => item.id === id), j = i + direction; if (i < 0 || j < 0 || j >= same.length) return false;
  const other = same[j], value = task.order; task.order = other.order; other.order = value; await markRecordDirty(task); await markRecordDirty(other); return true;
}
export async function pendingSummary() {
  const tasks = await getTasks(); let oneTime = 0, recurringDebt = 0, recurringDueTasks = 0;
  tasks.forEach(task => { if (task.taskKind === 'one_time') oneTime += 1; else { const debt = recurrenceState(task).debt; recurringDebt += debt; if (debt > 0) recurringDueTasks += 1; } });
  return { oneTime, recurringDebt, recurringDueTasks, totalDebtUnits: oneTime + recurringDebt, taskCount: tasks.length };
}
export async function pendingEntries() { const tasks = await getTasks(); return tasks.filter(t => t.taskKind === 'one_time' || recurrenceState(t).debt > 0); }
export async function importLegacyState(payload) {
  const today = localDateIso(); let imported = 0;
  if (payload?.format === 'taskium_import' && payload?.version === 1 && Array.isArray(payload.tasks)) {
    for (const item of payload.tasks) { const text = String(item?.title || '').trim(); if (!text) continue; await addTask(text, item.task_type === 'daily' ? 'recurring' : 'one_time', 1); imported += 1; }
    return { imported, settings:false };
  }
  if (!Array.isArray(payload?.tasks)) throw new Error('El JSON no parece pertenecer a Taskium.');
  const existing = await getTasks(); let orderBase = existing.length ? Math.max(...existing.map(t => Number(t.order) || 0)) + 10 : Date.now();
  for (const item of payload.tasks) {
    if (!item?.is_active && item?.is_active !== undefined) continue; const text = String(item?.text || item?.title || '').trim(); if (!text) continue;
    if (item.task_type === 'daily') {
      const anchor = String(item.created_at || today).slice(0,10); const last = String(item.last_processed_date || anchor).slice(0,10); const debtStored = Math.max(0, Number.parseInt(item.debt,10) || 0); const unprocessed = daysBetween(last, today); const currentDebt = debtStored + unprocessed; const generated = 1 + daysBetween(anchor, today); const completed = Math.max(0, generated - currentDebt);
      const task = normalizeTask({ id:uuid(), taskKind:'recurring', text, intervalDays:1, anchorDate:anchor, completedCount:completed, order:orderBase++, createdAt:item.created_at ? `${item.created_at}T12:00:00.000Z` : nowIso(), updatedAt:nowIso(), _sync:{dirty:true} }); await putRecord(task);
    } else { const task = normalizeTask({ id:uuid(), taskKind:'one_time', text, order:orderBase++, createdAt:item.created_at ? `${item.created_at}T12:00:00.000Z` : nowIso(), updatedAt:nowIso(), _sync:{dirty:true} }); await putRecord(task); }
    imported += 1;
  }
  let settingsImported = false;
  if (payload.settings && typeof payload.settings === 'object') { const current = await getSettings(); const migrated = normalizeSettingsRecord({ ...current, ...payload.settings, _sync: current._sync }); migrated.id = 'settings:main'; migrated.createdAt = current.createdAt; migrated.updatedAt = nowIso(); await markRecordDirty(migrated); settingsImported = true; }
  return { imported, settings:settingsImported };
}
export async function exportTaskium() { const tasks = await getTasks(), settings = await getSettings(); return { format:'taskium_v2_export', version:2, exportedAt:nowIso(), tasks:tasks.map(({_sync,...rest})=>rest), settings:(()=>{const {_sync,...rest}=settings;return rest;})() }; }
