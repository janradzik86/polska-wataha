-- Siatka neighborhood mesh schema

create table if not exists profiles (
  id text primary key,
  user_id text unique,
  display_name text not null,
  bio text not null default '',
  district text not null default '',
  lat double precision,
  lng double precision,
  reputation integer not null default 0,
  avatar_hue integer not null default 160,
  is_seed boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists listings (
  id text primary key,
  author_id text not null references profiles(id),
  kind text not null,
  title text not null,
  body text not null,
  category text not null,
  lat double precision,
  lng double precision,
  district text not null default '',
  status text not null default 'open',
  created_at timestamptz not null default now()
);

create table if not exists help_posts (
  id text primary key,
  author_id text not null references profiles(id),
  kind text not null,
  title text not null,
  body text not null,
  urgency text not null default 'normal',
  lat double precision,
  lng double precision,
  district text not null default '',
  status text not null default 'open',
  created_at timestamptz not null default now()
);

create table if not exists threads (
  id text primary key,
  listing_id text,
  title text not null default '',
  created_at timestamptz not null default now()
);

create table if not exists thread_members (
  thread_id text not null references threads(id) on delete cascade,
  profile_id text not null references profiles(id),
  primary key (thread_id, profile_id)
);

create table if not exists messages (
  id text primary key,
  thread_id text not null references threads(id) on delete cascade,
  sender_id text not null references profiles(id),
  body text not null,
  created_at timestamptz not null default now()
);

create table if not exists exchange_offers (
  id text primary key,
  listing_id text not null references listings(id),
  from_id text not null references profiles(id),
  to_id text not null references profiles(id),
  offer_text text not null,
  status text not null default 'pending',
  created_at timestamptz not null default now()
);

create table if not exists badges (
  id text primary key,
  slug text not null unique,
  title text not null,
  description text not null
);

create table if not exists profile_badges (
  profile_id text not null references profiles(id),
  badge_id text not null references badges(id),
  earned_at timestamptz not null default now(),
  primary key (profile_id, badge_id)
);

create table if not exists crisis_alerts (
  id text primary key,
  author_id text not null references profiles(id),
  kind text not null,
  body text not null,
  lat double precision,
  lng double precision,
  created_at timestamptz not null default now()
);

create table if not exists mesh_nodes (
  id text primary key,
  label text not null,
  kind text not null,
  lat double precision not null,
  lng double precision not null,
  status text not null default 'online',
  last_seen timestamptz not null default now()
);

create table if not exists help_points (
  id text primary key,
  title text not null,
  kind text not null,
  lat double precision not null,
  lng double precision not null,
  note text not null default ''
);

create table if not exists sync_queue (
  id text primary key,
  user_id text not null,
  action text not null,
  payload text not null,
  created_at timestamptz not null default now(),
  synced_at timestamptz
);

create table if not exists reputation_events (
  id text primary key,
  profile_id text not null references profiles(id),
  delta integer not null,
  reason text not null,
  created_at timestamptz not null default now()
);

create index if not exists listings_created_idx on listings (created_at desc);
create index if not exists help_posts_created_idx on help_posts (created_at desc);
create index if not exists messages_thread_idx on messages (thread_id, created_at);
create index if not exists crisis_created_idx on crisis_alerts (created_at desc);
create index if not exists profiles_user_id_idx on profiles (user_id);
