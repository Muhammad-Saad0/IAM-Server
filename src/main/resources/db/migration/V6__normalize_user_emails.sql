do $$
begin
    if exists (
        select lower(email)
        from users
        group by lower(email)
        having count(*) > 1
    ) then
        raise exception 'Cannot normalize user emails because case-insensitive duplicates exist';
    end if;
end
$$;

update users
set email = lower(email)
where email <> lower(email);

alter table users
    drop constraint if exists users_email_key;

create unique index uq_users_email_case_insensitive
    on users (lower(email));

alter table users
    add constraint chk_users_email_lowercase
    check (email = lower(email));
