insert into roles (name, created_at)
values
    ('ADMIN', now()),
    ('USER', now())
on conflict (name) do nothing;
