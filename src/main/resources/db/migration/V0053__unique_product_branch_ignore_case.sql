-- Запрет на ветки, различающиеся только регистром.
--
-- uq_product_branch_alias_branch_name регистрозависимый, поэтому main и Main — разные строки.
-- Код после SFDM-4043 такую пару не создаёт: поиск ветки регистронезависимый, а имя пишется в
-- нижнем регистре. Но запись мимо API — прямой INSERT, миграция данных, другой сервис — по-прежнему
-- может, и тогда часть методов перестанет находить ветку. Здесь это закрывается на уровне БД.
--
-- Безопасно к применению: на момент добавления все branch_name в нижнем регистре, значит
-- lower(branch_name) = branch_name, и пара (alias, lower(branch_name)) уже уникальна по
-- действующему индексу — новый индекс не может конфликтовать с данными.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_branch_alias_lower_branch_name
    ON product.product_branch (alias, lower(branch_name));

-- Старый индекс намеренно остаётся: на него опирается ON CONFLICT (alias, branch_name) в
-- ProductBranchRepository.upsert — для ON CONFLICT нужен уникальный индекс ровно по этим колонкам,
-- по выражению lower(branch_name) он не подойдёт.
