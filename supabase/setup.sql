-- Referencia reproducible de la infraestructura Taskium 2.
create table if not exists public.taskium_records (
  user_id uuid not null references auth.users(id) on delete cascade,
  record_id text not null,
  record_type text not null check (record_type in ('task','settings')),
  payload jsonb not null default '{}'::jsonb,
  client_updated_at timestamptz,
  server_updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  primary key (user_id, record_id)
);
create table if not exists public.taskium_push_subscriptions (
  user_id uuid not null references auth.users(id) on delete cascade,
  endpoint text not null,p256dh text not null,auth text not null,user_agent text,
  created_at timestamptz not null default now(),updated_at timestamptz not null default now(),
  primary key (user_id, endpoint)
);
create table if not exists public.taskium_alarm_deliveries (
  user_id uuid not null references auth.users(id) on delete cascade,
  delivery_key text not null,created_at timestamptz not null default now(),
  primary key (user_id, delivery_key)
);
alter table public.taskium_records enable row level security;
alter table public.taskium_push_subscriptions enable row level security;
alter table public.taskium_alarm_deliveries enable row level security;
-- Las políticas RLS de producción ya fueron aplicadas en el proyecto conectado.
-- La Edge Function taskium-push-tick y el cron de un minuto también fueron desplegados en producción.
