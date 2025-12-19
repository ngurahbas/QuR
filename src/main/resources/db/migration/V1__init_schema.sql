create or replace function update_updated_at_column()
    returns trigger as
$$
begin
    NEW.updated_at = CURRENT_TIMESTAMP;
    return NEW;
end;
$$ language plpgsql;

create table identifier
(
    id         bigserial primary key,
    type       varchar(10) check ( type in ('EMAIL', 'MOBILE')) not null,
    value      varchar(255)                                     not null,
    created_at TIMESTAMPTZ                                      NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ                                      NOT NULL DEFAULT now()
);

drop trigger if exists set_updated_at_identifier on identifier;
create trigger set_updated_at_identifier
    before update
    on identifier
    for each row
execute procedure update_updated_at_column();