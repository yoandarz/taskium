import { DAYS, DEFAULT_SNOOZE_MINUTES, RECORD_TYPES, TASK_KINDS } from './constants.js';
import { clampInt, localDateIso, nowIso } from './utils.js';
function syncState(input = {}) { return { dirty: Boolean(input.dirty), deleted: Boolean(input.deleted), syncedServerUpdatedAt: input.syncedServerUpdatedAt || null, seed: Boolean(input.seed) }; }
export function defaultAlarmDays() { return DAYS.map(day => ({ day_name: day.key, slots: Array.from({ length: 3 }, () => ({ enabled: false, hour: 9, minute: 0 })) })); }
export function defaultSettingsPayload() {
  return {
    theme: 'light', snoozeMinutes: DEFAULT_SNOOZE_MINUTES,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Madrid',
    alarmsByDay: defaultAlarmDays(), snoozeUntil: null,
  };
}
export function normalizeSettingsRecord(input = {}) {
  const defaults = defaultSettingsPayload(); const source = input.payload && input.type === 'settings' ? input.payload : input;
  const alarms = Array.isArray(source.alarmsByDay) ? source.alarmsByDay : Array.isArray(source.alarms_by_day) ? source.alarms_by_day : defaults.alarmsByDay;
  const mapped = DAYS.map((day, index) => {
    const found = alarms.find(item => String(item?.day_name || '').toLowerCase() === day.key) || alarms[index] || {};
    const slots = Array.from({ length: 3 }, (_, slotIndex) => { const slot = found.slots?.[slotIndex] || {}; return { enabled: Boolean(slot.enabled), hour: clampInt(slot.hour, 0, 23, 9), minute: clampInt(slot.minute, 0, 59, 0) }; });
    return { day_name: day.key, slots };
  });
  return {
    id: 'settings:main', type: 'settings',
    theme: ['light','dark'].includes(source.theme) ? source.theme : source.theme_name === 'taskium_dark' ? 'dark' : 'light',
    snoozeMinutes: clampInt(source.snoozeMinutes ?? source.snooze_minutes, 1, 60, DEFAULT_SNOOZE_MINUTES),
    timezone: String(source.timezone || defaults.timezone), alarmsByDay: mapped,
    snoozeUntil: source.snoozeUntil || null,
    createdAt: input.createdAt || nowIso(), updatedAt: input.updatedAt || nowIso(),
    _sync: syncState(input._sync),
  };
}
export function normalizeTask(input = {}) {
  const kindRaw = input.taskKind || (input.task_type === 'daily' ? 'recurring' : input.task_type === 'one_time' ? 'one_time' : input.taskKind);
  const taskKind = TASK_KINDS.includes(kindRaw) ? kindRaw : 'one_time';
  const today = localDateIso();
  const record = {
    id: String(input.id || crypto.randomUUID()), type: 'task', taskKind,
    text: String(input.text || input.title || '').trim(),
    order: Number.isFinite(Number(input.order)) ? Number(input.order) : Date.now(),
    createdAt: input.createdAt || input.created_at || nowIso(), updatedAt: input.updatedAt || nowIso(),
    _sync: syncState(input._sync),
  };
  if (taskKind === 'recurring') {
    record.intervalDays = clampInt(input.intervalDays ?? input.interval_days, 1, 3650, 1);
    record.anchorDate = String(input.anchorDate || input.anchor_date || input.created_at || today).slice(0, 10) || today;
    record.completedCount = Math.max(0, Number.parseInt(input.completedCount ?? input.completed_count, 10) || 0);
  }
  return record;
}
export function normalizeRecord(input = {}) {
  if (input.type === 'settings' || input.record_type === 'settings' || input.id === 'settings:main') return normalizeSettingsRecord(input);
  if (input.type === 'task' || input.record_type === 'task' || input.taskKind || input.task_type) return normalizeTask(input);
  throw new Error('Tipo de registro Taskium no reconocido.');
}
export function validateRecord(record) { return RECORD_TYPES.includes(record?.type) && (record.type !== 'task' || Boolean(record.text)); }
