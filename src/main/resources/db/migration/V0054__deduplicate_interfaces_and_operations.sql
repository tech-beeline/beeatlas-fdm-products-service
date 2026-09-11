DO
$$
    DECLARE
        dup       RECORD;
        loser     RECORD;
        op        RECORD;
        twin      RECORD;
        winner_id INTEGER;
        moved     INTEGER := 0;
        dropped   INTEGER := 0;
    BEGIN
        -- 1. Дубли интерфейсов внутри контейнера
        FOR dup IN
            SELECT container_id, code
            FROM product.interface
            WHERE code IS NOT NULL
            GROUP BY container_id, code
            HAVING count(*) > 1
            LOOP
                SELECT i.id
                INTO winner_id
                FROM product.interface i
                WHERE i.container_id = dup.container_id
                  AND i.code = dup.code
                ORDER BY (i.deleted_date IS NULL) DESC,
                         (SELECT count(*)
                          FROM product.operation o
                          WHERE o.interface_id = i.id
                            AND o.deleted_date IS NULL) DESC,
                         (SELECT count(*)
                          FROM product.operation o
                                   JOIN product.operation_relations r
                                        ON r.operation_id = o.id OR r.related_operation_id = o.id
                          WHERE o.interface_id = i.id) DESC,
                         i.id
                LIMIT 1;

                FOR loser IN
                    SELECT id
                    FROM product.interface
                    WHERE container_id = dup.container_id
                      AND code = dup.code
                      AND id <> winner_id
                    LOOP
                        FOR op IN SELECT * FROM product.operation WHERE interface_id = loser.id
                            LOOP
                                SELECT *
                                INTO twin
                                FROM product.operation w
                                WHERE w.interface_id = winner_id
                                  AND w.name IS NOT DISTINCT FROM op.name
                                  AND w.type IS NOT DISTINCT FROM op.type
                                ORDER BY w.id
                                LIMIT 1;

                                IF NOT FOUND THEN
                                    -- у выжившей такого метода нет: операция переезжает целиком,
                                    -- вместе со связями, sla и параметрами
                                    UPDATE product.operation SET interface_id = winner_id WHERE id = op.id;
                                    moved := moved + 1;
                                ELSE
                                    UPDATE product.operation_relations
                                    SET operation_id = twin.id
                                    WHERE operation_id = op.id;
                                    UPDATE product.operation_relations
                                    SET related_operation_id = twin.id
                                    WHERE related_operation_id = op.id;
                                    UPDATE product.discovered_operation
                                    SET connection_operation_id = twin.id
                                    WHERE connection_operation_id = op.id;
                                    -- живой метод не должен раствориться в удалённом двойнике
                                    IF op.deleted_date IS NULL AND twin.deleted_date IS NOT NULL THEN
                                        UPDATE product.operation
                                        SET deleted_date = NULL,
                                            updated_date = now()
                                        WHERE id = twin.id;
                                    END IF;
                                    DELETE FROM product.parameter WHERE operation_id = op.id;
                                    DELETE FROM product.sla WHERE operation_id = op.id;
                                    DELETE FROM product.operation WHERE id = op.id;
                                    dropped := dropped + 1;
                                END IF;
                            END LOOP;

                        UPDATE product.discovered_interface
                        SET connection_interface_id = winner_id
                        WHERE connection_interface_id = loser.id;

                        DELETE FROM product.interface WHERE id = loser.id;
                        RAISE NOTICE 'interface: containerId=%, code=%, оставлен id=%, удалён id=%',
                            dup.container_id, dup.code, winner_id, loser.id;
                    END LOOP;
            END LOOP;

        RAISE NOTICE 'Дубли interface схлопнуты: операций перенесено %, операций слито %', moved, dropped;

        moved := 0;
        FOR dup IN
            SELECT interface_id, name, COALESCE(type, '') AS type_key
            FROM product.operation
            GROUP BY interface_id, name, COALESCE(type, '')
            HAVING count(*) > 1
            LOOP
                SELECT o.id
                INTO winner_id
                FROM product.operation o
                WHERE o.interface_id = dup.interface_id
                  AND o.name IS NOT DISTINCT FROM dup.name
                  AND COALESCE(o.type, '') = dup.type_key
                ORDER BY (o.deleted_date IS NULL) DESC,
                         (SELECT count(*)
                          FROM product.operation_relations r
                          WHERE r.operation_id = o.id
                             OR r.related_operation_id = o.id) DESC,
                         o.id
                LIMIT 1;

                FOR op IN
                    SELECT id
                    FROM product.operation
                    WHERE interface_id = dup.interface_id
                      AND name IS NOT DISTINCT FROM dup.name
                      AND COALESCE(type, '') = dup.type_key
                      AND id <> winner_id
                    LOOP
                        UPDATE product.operation_relations SET operation_id = winner_id WHERE operation_id = op.id;
                        UPDATE product.operation_relations
                        SET related_operation_id = winner_id
                        WHERE related_operation_id = op.id;
                        UPDATE product.discovered_operation
                        SET connection_operation_id = winner_id
                        WHERE connection_operation_id = op.id;
                        DELETE FROM product.parameter WHERE operation_id = op.id;
                        DELETE FROM product.sla WHERE operation_id = op.id;
                        DELETE FROM product.operation WHERE id = op.id;
                        moved := moved + 1;
                        RAISE NOTICE 'operation: interfaceId=%, name=%, type=%, оставлена id=%, удалена id=%',
                            dup.interface_id, dup.name, dup.type_key, winner_id, op.id;
                    END LOOP;
            END LOOP;

        RAISE NOTICE 'Дубли operation схлопнуты: удалено %', moved;
    END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_interface_container_id_code
    ON product.interface (container_id, code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_operation_interface_id_name_type
    ON product.operation (interface_id, name, COALESCE(type, ''));
