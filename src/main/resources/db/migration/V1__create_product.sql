create table product (
    id       bigint generated always as identity primary key,
    name     varchar(45) not null,
    quantity integer     not null default 0,
    version  bigint      not null default 0,
    constraint product_name_not_blank check (length(btrim(name)) > 0),
    constraint product_quantity_range check (quantity between 0 and 999999999)
);

-- Nome único sem diferenciar maiúsculas e minúsculas.
create unique index product_name_unique on product (lower(name));
