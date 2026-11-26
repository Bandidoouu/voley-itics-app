create table entrenador (
    id      uuid primary key references auth.users (id) on delete cascade,
    usuario text not null unique
);

create table jugador (
    id            bigint generated always as identity primary key,
    entrenador_id uuid    not null default auth.uid() references entrenador (id) on delete cascade,
    nombre        text    not null,
    numero        int     not null check (numero between 0 and 99),
    posicion      text    not null check (posicion in ('ARMADOR', 'OPUESTO', 'PUNTA', 'CENTRAL', 'LIBERO')),
    unique (entrenador_id, numero)
);

create table partido (
    id            bigint generated always as identity primary key,
    entrenador_id uuid    not null default auth.uid() references entrenador (id) on delete cascade,
    rival         text    not null,
    fecha         date    not null default (now() at time zone 'America/Monterrey')::date,
    set_actual    int     not null default 1,
    terminado     boolean not null default false
);

create table jugada (
    id         bigint generated always as identity primary key,
    partido_id bigint not null references partido (id) on delete cascade,
    jugador_id bigint          references jugador (id) on delete cascade,
    set_num    int    not null,
    tipo       text   not null check (tipo in ('SAQUE', 'RECEPCION', 'ARMADO', 'ATAQUE', 'BLOQUEO', 'DEFENSA', 'RIVAL')),
    resultado  text   not null check (resultado in ('PUNTO', 'ERROR', 'CONTINUA')),
    check ((jugador_id is null) = (tipo = 'RIVAL'))
);

create index on partido (entrenador_id);
create index on jugada (partido_id, set_num);
create index on jugada (jugador_id);

create view marcador with (security_invoker = true) as
select partido_id, set_num,
       count(*) filter (where resultado = 'PUNTO') as nosotros,
       count(*) filter (where resultado = 'ERROR') as rival
from jugada
group by partido_id, set_num;

create view estadistica with (security_invoker = true) as
select jugador_id, tipo,
       count(*)                                    as total,
       count(*) filter (where resultado = 'PUNTO') as puntos,
       count(*) filter (where resultado = 'ERROR') as errores
from jugada
where jugador_id is not null
group by jugador_id, tipo;

create view estadistica_partido with (security_invoker = true) as
select partido_id, jugador_id, tipo,
       count(*)                                    as total,
       count(*) filter (where resultado = 'PUNTO') as puntos,
       count(*) filter (where resultado = 'ERROR') as errores
from jugada
where jugador_id is not null
group by partido_id, jugador_id, tipo;

alter table entrenador enable row level security;
alter table jugador    enable row level security;
alter table partido    enable row level security;
alter table jugada     enable row level security;

create policy propio on entrenador for select to authenticated
    using (id = auth.uid());

create policy propio on jugador for all to authenticated
    using (entrenador_id = auth.uid())
    with check (entrenador_id = auth.uid());

create policy propio on partido for all to authenticated
    using (entrenador_id = auth.uid())
    with check (entrenador_id = auth.uid());

create policy propio on jugada for all to authenticated
    using (exists (select 1 from partido p where p.id = partido_id and p.entrenador_id = auth.uid()))
    with check (
        exists (select 1 from partido p where p.id = partido_id and p.entrenador_id = auth.uid())
        and (jugador_id is null or exists (select 1 from jugador j where j.id = jugador_id and j.entrenador_id = auth.uid()))
    );

create function siguiente_set(p_partido bigint) returns void
language sql security invoker as $$
    update partido set set_actual = set_actual + 1 where id = p_partido;
$$;

create function deshacer(p_partido bigint) returns void
language sql security invoker as $$
    delete from jugada where id = (
        select max(j.id) from jugada j join partido p on p.id = j.partido_id
        where j.partido_id = p_partido and j.set_num = p.set_actual
    );
$$;

create function nuevo_entrenador() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.entrenador (id, usuario)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'usuario', new.email));
    return new;
end;
$$;

create trigger al_registrar after insert on auth.users
    for each row execute function nuevo_entrenador();

revoke all on entrenador, jugador, partido, jugada, marcador, estadistica, estadistica_partido from anon;
grant select on entrenador to authenticated;
grant select, insert, update, delete on jugador, partido, jugada to authenticated;
grant select on marcador, estadistica, estadistica_partido to authenticated;
revoke execute on function siguiente_set(bigint), deshacer(bigint), nuevo_entrenador() from public, anon;
grant execute on function siguiente_set(bigint), deshacer(bigint) to authenticated;
