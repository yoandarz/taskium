const WINDOWS_BRIDGE = 'http://127.0.0.1:51337';

function cleanRecord(record) {
  if (!record || typeof record !== 'object') return record;
  const out = { ...record };
  delete out._sync;
  return out;
}

function shiftIsoDate(iso, days) {
  const d = new Date(`${iso}T00:00:00Z`);
  if (Number.isNaN(d.getTime())) return iso;
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

// Compatibility adapter for the already-installed native engines.
// Web semantics: anchorDate is the FIRST due date.
// Older native engines count one unit immediately from their anchor,
// so we shift the bridge-only anchor back one interval and add one
// completed unit. Stored/cloud task data is not modified.
function taskForNativeBridge(record) {
  const out = cleanRecord(record);
  if (out?.taskKind === 'recurring' && out.anchorDate) {
    const interval = Math.max(1, Number(out.intervalDays) || 1);
    out.anchorDate = shiftIsoDate(out.anchorDate, -interval);
    out.completedCount = Math.max(0, Number(out.completedCount) || 0) + 1;
  }
  return out;
}

function androidBridge() {
  return window.TaskiumNativeAndroid && typeof window.TaskiumNativeAndroid.syncState === 'function'
    ? window.TaskiumNativeAndroid
    : null;
}

async function windowsHealth(timeoutMs = 450) {
  const ctl = new AbortController();
  const timer = setTimeout(() => ctl.abort(), timeoutMs);
  try {
    const r = await fetch(`${WINDOWS_BRIDGE}/health`, { signal: ctl.signal, cache: 'no-store' });
    if (!r.ok) return null;
    return await r.json();
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

export async function alarmBackend() {
  if (androidBridge()) return { kind: 'android', available: true, label: 'Android · alarma local' };
  const win = await windowsHealth();
  if (win?.ok) return { kind: 'windows', available: true, label: 'Windows · alarma local' };
  return { kind: 'web', available: false, label: 'Web · sin alarma local' };
}

export async function activateLocalAlarms() {
  const android = androidBridge();
  if (android) {
    android.requestAlarmPermissions();
    return { kind: 'android', ok: true };
  }
  const win = await windowsHealth(900);
  if (win?.ok) return { kind: 'windows', ok: true };
  throw new Error('En Windows instala y ejecuta Taskium Alarm Bridge. En Android usa la aplicación nativa de Taskium.');
}

export async function reconcileLocalAlarms(settings, tasks) {
  const payload = JSON.stringify({
    version: 2,
    updatedAt: new Date().toISOString(),
    settings: cleanRecord(settings),
    tasks: (tasks || []).map(taskForNativeBridge),
  });

  const android = androidBridge();
  if (android) {
    const result = android.syncState(payload);
    return { kind: 'android', ok: result !== 'error' };
  }

  const win = await windowsHealth();
  if (win?.ok) {
    const r = await fetch(`${WINDOWS_BRIDGE}/state`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
    });
    if (!r.ok) throw new Error('No se pudo actualizar el reloj local de Windows.');
    return { kind: 'windows', ok: true };
  }

  return { kind: 'web', ok: false };
}
