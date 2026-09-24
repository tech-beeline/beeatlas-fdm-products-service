-- Дефект QA-3: POST /api/v2/e2e отвечал 500 (org.hibernate.exception.DataException:
-- "could not execute statement") на публикации из staging-service.
--
-- Причина — колонки, в которые пишет sparx-путь, унаследовали узкие лимиты от MAPIC-импорта:
-- значение из Sparx (тип операции = первое слово имени, api-ссылка, описание, версия) в них
-- просто не влезает, и Postgres отвечает "value too long for type character varying(N)".
-- Прикладной смысл ограничивать эти поля отсутствует — расширяем до text, как уже сделано
-- для name в V0045. varchar(n) -> text в Postgres binary coercible, перезаписи таблицы нет.
ALTER TABLE product.discovered_operation ALTER COLUMN type        TYPE text;
ALTER TABLE product.discovered_operation ALTER COLUMN return_type TYPE text;
ALTER TABLE product.discovered_operation ALTER COLUMN description TYPE text;

ALTER TABLE product.discovered_interface ALTER COLUMN api_link    TYPE text;
ALTER TABLE product.discovered_interface ALTER COLUMN version     TYPE text;
ALTER TABLE product.discovered_interface ALTER COLUMN description TYPE text;

-- Второй способ получить 500 на том же эндпоинте: связь вызовов без стереотипа. В Sparx
-- стереотип у сообщения опциональный, staging передаёт null, а колонка была NOT NULL без
-- дефолта — вставка падала уже на уровне БД, до всякой валидации.
ALTER TABLE product.operation_relations ALTER COLUMN stereo_type DROP NOT NULL;
