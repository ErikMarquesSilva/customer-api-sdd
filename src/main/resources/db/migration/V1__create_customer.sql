CREATE TABLE customer (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(254) NOT NULL,
    cpf         VARCHAR(11)  NOT NULL,
    phone       VARCHAR(14),
    birth_date  DATE,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(2)   NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL,
    CONSTRAINT uk_customer_email UNIQUE (email),
    CONSTRAINT uk_customer_cpf   UNIQUE (cpf)
);
