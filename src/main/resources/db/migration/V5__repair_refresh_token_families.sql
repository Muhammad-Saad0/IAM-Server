-- Older rotation code assigned a new family ID to every replacement token.
-- Rebuild each replacement chain so all descendants share the root token's
-- family ID. This lets replay detection revoke every token in an existing chain.
with recursive token_chains as (
    select
        refresh_token.id,
        refresh_token.token_family_id as root_family_id
    from refresh_tokens refresh_token
    where not exists (
        select 1
        from refresh_tokens parent_token
        where parent_token.replaced_by_token_id = refresh_token.id
    )

    union all

    select
        replacement_token.id,
        token_chains.root_family_id
    from token_chains
    join refresh_tokens current_token
        on current_token.id = token_chains.id
    join refresh_tokens replacement_token
        on replacement_token.id = current_token.replaced_by_token_id
)
update refresh_tokens refresh_token
set token_family_id = token_chains.root_family_id
from token_chains
where refresh_token.id = token_chains.id
  and refresh_token.token_family_id <> token_chains.root_family_id;
