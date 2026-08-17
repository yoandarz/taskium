export function nowIso() { return new Date().toISOString(); }
export function uuid() { return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`; }
export function clampInt(value, min, max, fallback = min) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}
export function localDateIso(date = new Date()) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}
export function parseIsoDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || '').trim());
  if (!match) return null;
  const [, y, m, d] = match.map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== m - 1 || date.getUTCDate() !== d) return null;
  return date;
}
export function daysBetween(startIso, endIso = localDateIso()) {
  const start = parseIsoDate(startIso), end = parseIsoDate(endIso);
  if (!start || !end) return 0;
  return Math.max(0, Math.floor((end.getTime() - start.getTime()) / 86400000));
}
export function addDays(iso, days) {
  const date = parseIsoDate(iso);
  if (!date) return iso;
  date.setUTCDate(date.getUTCDate() + Number(days || 0));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
}
export function formatDate(iso) {
  const date = parseIsoDate(iso);
  if (!date) return '—';
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' }).format(new Date(date.getTime() + 12 * 3600000));
}
export function escapeHtml(value = '') {
  return String(value).replace(/[&<>'"]/g, char => ({ '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;' }[char]));
}
export function recurrenceState(task, today = localDateIso()) {
  if (task?.taskKind !== 'recurring') return { generatedCount: 0, completedCount: 0, debt: 0, nextDueDate: null };
  const intervalDays = clampInt(task.intervalDays, 1, 3650, 1);
  const anchorDate = parseIsoDate(task.anchorDate) ? task.anchorDate : today;
  const elapsed = daysBetween(anchorDate, today);
  const generatedCount = 1 + Math.floor(elapsed / intervalDays);
  const completedCount = Math.max(0, Number.parseInt(task.completedCount, 10) || 0);
  const debt = Math.max(0, generatedCount - completedCount);
  const nextDueDate = addDays(anchorDate, generatedCount * intervalDays);
  return { intervalDays, anchorDate, elapsed, generatedCount, completedCount, debt, nextDueDate };
}
export function downloadJson(data, filename) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 500);
}
export function sameJson(a, b) { return JSON.stringify(a) === JSON.stringify(b); }
