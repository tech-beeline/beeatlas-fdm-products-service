DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM product.local_fitness_function LIMIT 1)
    THEN
       INSERT INTO product.local_fitness_function (id, code, description, status, doc_link)
        VALUES
            (1, 'ADR.01', 'Наличие хотя бы одного ADR', 'default', NULL),
            (2, 'API.01', 'У приложения есть опубликованные API', 'default', NULL),
            (3, 'API.02', 'Для некоторых методов определен SLA', 'default', NULL),
            (4, 'API.03', 'Для всех TC есть спецификация', 'default', NULL),
            (5, 'CPB.01', 'Определены технические возможности продукта', 'default', NULL),
            (6, 'CBP.02', 'Для всех внешних интеграций определены TC', 'default', NULL),
            (7, 'CNT.01', 'Наличие в модели контейнеров для системы', 'default', NULL),
            (8, 'CNT.02', 'Наличие в хотя бы одной диаграмме контейнеров', 'default', NULL),
            (9, 'CNT.03', 'Все вызовы между контейнерами имеют технологию', 'default', NULL),
            (10, 'CTX.01', 'Создана диаграмма контекста', 'default', NULL),
            (11, 'CTX.02', 'Все связи на диаграмме контекста должны быть подписаны', 'default', NULL),
            (12, 'CTX.03', 'Все связи на диаграмме контекста должны иметь технологию взаимодействия', 'default', NULL),
            (13, 'DEP.01', 'Наличие хотя бы одного Deployment Environment', 'default', NULL),
            (14, 'DEP.02', 'Наличие хотя бы одной Deployment диаграммы', 'default', NULL),
            (15, 'DEP.03', 'Deployment Environment соответствует CMDB', 'default', NULL),
            (16, 'DEP.04', 'Правильно задана макросегментация Protected/DMZ STD/NST Operations/RND', 'default', NULL),
            (17, 'SQ.01', 'Для всех technical capability указаны sequence', 'default', NULL),
            (18, 'SQ.02', 'Все вызовы содержат HTTP запросы', 'default', NULL),
            (19, 'TECH.01', 'Все технологии продукта есть в техрадаре', 'default', NULL),
            (20, 'TECH.02', 'В продукте нет технологий в статусе HOLD', 'default', NULL),
            (21, 'TECH.03', 'у всех контейнеров есть технологии', 'default', NULL),
            (22, 'TECH.04', 'Приложение не использует протоколов в статусе hold', 'default', NULL),
            (23, 'TECH.05', 'У всех взаимодействий указаны протоколы из техрадара', 'default', NULL),
            (24, 'TECH.06', 'Все технологии найденные мониторингом описаны в архитектуре', 'default', NULL),
            (25, 'GIT.01', 'У всех контенеров указан Git репозиторий', 'default', NULL),
            (26, 'SEC.01', 'Все системы корректно интегрированы с IDM', 'default', NULL),
            (27, 'CPB.04', 'Позиционирование системы произведено правильно', 'default', NULL),
            (28, 'CPB.05', 'Технические возможности описаны в соответствии с методикой', 'default', NULL),
            (30, 'PAT.01', 'Отсутствие взаимодействия между системами черз технологию db_link', 'default', NULL),
            (31, 'CJ.001', 'Есть хотя бы один опубликованный CJ у приложения', 'default', NULL);
END IF;
END $$;