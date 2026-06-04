-- V2 — Corrige le type de la colonne currency.
-- V1 la créait en CHAR(3) (bpchar côté PostgreSQL) alors que l'entité JPA mappe
-- String -> VARCHAR : Hibernate (ddl-auto=validate) refusait de démarrer
-- ("wrong column type encountered in column [currency] ... found [bpchar]").
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3);
