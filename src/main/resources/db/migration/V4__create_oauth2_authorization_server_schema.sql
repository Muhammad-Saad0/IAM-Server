-- PostgreSQL version of Spring Authorization Server 1.5 JDBC schema.
-- The upstream schema's blob columns are represented as text for PostgreSQL.

create table oauth2_registered_client (
    id varchar(100) primary key,
    client_id varchar(100) not null,
    client_id_issued_at timestamp not null default current_timestamp,
    client_secret varchar(200) null,
    client_secret_expires_at timestamp null,
    client_name varchar(200) not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types varchar(1000) not null,
    redirect_uris varchar(1000) null,
    post_logout_redirect_uris varchar(1000) null,
    scopes varchar(1000) not null,
    client_settings varchar(2000) not null,
    token_settings varchar(2000) not null
);

create unique index idx_oauth2_registered_client_client_id
    on oauth2_registered_client(client_id);

create table oauth2_authorization (
    id varchar(100) primary key,
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorization_grant_type varchar(100) not null,
    authorized_scopes varchar(1000) null,
    attributes text null,
    state varchar(500) null,
    authorization_code_value text null,
    authorization_code_issued_at timestamp null,
    authorization_code_expires_at timestamp null,
    authorization_code_metadata text null,
    access_token_value text null,
    access_token_issued_at timestamp null,
    access_token_expires_at timestamp null,
    access_token_metadata text null,
    access_token_type varchar(100) null,
    access_token_scopes varchar(1000) null,
    oidc_id_token_value text null,
    oidc_id_token_issued_at timestamp null,
    oidc_id_token_expires_at timestamp null,
    oidc_id_token_metadata text null,
    refresh_token_value text null,
    refresh_token_issued_at timestamp null,
    refresh_token_expires_at timestamp null,
    refresh_token_metadata text null,
    user_code_value text null,
    user_code_issued_at timestamp null,
    user_code_expires_at timestamp null,
    user_code_metadata text null,
    device_code_value text null,
    device_code_issued_at timestamp null,
    device_code_expires_at timestamp null,
    device_code_metadata text null
);

create index idx_oauth2_authorization_registered_client_id
    on oauth2_authorization(registered_client_id);
create index idx_oauth2_authorization_principal_name
    on oauth2_authorization(principal_name);
create index idx_oauth2_authorization_state
    on oauth2_authorization(state);

create table oauth2_authorization_consent (
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorities varchar(1000) not null,
    primary key (registered_client_id, principal_name)
);
