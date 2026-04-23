DO $$
DECLARE
v_product_id    integer;
  v_container_id  integer;
  v_interface_id  integer;
  v_operation_id  integer;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM product.product) THEN

    ----------------------------------------------------------------
    -- Продукт 1: Геологический портал / каталог данных
    ----------------------------------------------------------------
    INSERT INTO product.product (
      name, alias, description, git_url,
      structurizr_workspace_name, structurizr_api_key, structurizr_api_secret,
      source, upload_date, critical
    )
    VALUES (
      'Геологический портал',
      'geo-portal',
      'Витрина геологических и геофизических данных, работающих на ГИС и GDM.',
      'https://git.example.com/geo/portal.git',
      'geo-portal-workspace',
      'geo-portal-key',
      'geo-portal-secret',
      'migration',
      now(),
      'High'
    )
    RETURNING id INTO v_product_id;

INSERT INTO product.containers_product (
    name, product_id, code, version,
    created_date, updated_date, deleted_date
)
VALUES (
           'Geo Portal API',
           v_product_id,
           'geo-portal-api',
           '1.0.0',
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_container_id;

-- Привязываем к технической возможности: TC.00002 (ГИС платформа, id = 2)
INSERT INTO product.interface (
    name, code, spec_link, version,
    type_id, description, protocol, status_id,
    tc_id, container_id,
    created_date, deleted_date, updated_date
)
VALUES (
           'Geo Catalog REST API',
           'GEO-CATALOG-API',
           'https://api.example.com/geo-catalog/openapi',
           'v1',
           NULL,
           'REST API для доступа к геологическому каталогу (геоданные, слои, метаданные).',
           'HTTPS/REST',
           NULL,
           2,
           v_container_id,
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_interface_id;

-- Операция 1
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Получить список слоёв',
           'Возвращает список доступных геопространственных слоёв для заданного региона.',
           'QUERY',
           'application/json',
           now(),
           NULL,
           NULL,
           2,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 50.00000, 300.00000, 0.01000);

-- Операция 2
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Получить метаданные слоя',
           'Возвращает метаданные и структуру выбранного слоя/набора геоданных.',
           'QUERY',
           'application/json',
           now(),
           NULL,
           NULL,
           2,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 20.00000, 400.00000, 0.00500);

-- Операция 3
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Поиск объектов по полигону',
           'Поиск геообъектов внутри заданного полигона (пространственный запрос).',
           'QUERY',
           'application/json',
           now(),
           NULL,
           NULL,
           2,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 10.00000, 800.00000, 0.01000);


----------------------------------------------------------------
-- Продукт 2: Система управления бурением
----------------------------------------------------------------
INSERT INTO product.product (
    name, alias, description, git_url,
    structurizr_workspace_name, structurizr_api_key, structurizr_api_secret,
    source, upload_date, critical
)
VALUES (
           'Система управления бурением',
           'drilling-mgmt',
           'Цифровое планирование, мониторинг и анализ буровых работ.',
           'https://git.example.com/drilling/mgmt.git',
           'drilling-mgmt-workspace',
           'drilling-mgmt-key',
           'drilling-mgmt-secret',
           'migration',
           now(),
           'Critical'
       )
    RETURNING id INTO v_product_id;

INSERT INTO product.containers_product (
    name, product_id, code, version,
    created_date, updated_date, deleted_date
)
VALUES (
           'Drilling Control API',
           v_product_id,
           'drilling-control-api',
           '1.0.0',
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_container_id;

-- Привязываем к технической возможности: TC.00007 (Управление бурением, id = 7)
INSERT INTO product.interface (
    name, code, spec_link, version,
    type_id, description, protocol, status_id,
    tc_id, container_id,
    created_date, deleted_date, updated_date
)
VALUES (
           'Drilling Control REST API',
           'DRILLING-CTRL-API',
           'https://api.example.com/drilling/openapi',
           'v1',
           NULL,
           'REST API для планирования и мониторинга буровых операций и скважин.',
           'HTTPS/REST',
           NULL,
           7,
           v_container_id,
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_interface_id;

-- Операция 1
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Создать план бурения',
           'Создание и сохранение плана бурения для заданного месторождения/скважины.',
           'COMMAND',
           'application/json',
           now(),
           NULL,
           NULL,
           7,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 5.00000, 1500.00000, 0.02000);

-- Операция 2
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Получить статус бурения',
           'Возвращает текущий статус работ по скважине и ключевые показатели.',
           'QUERY',
           'application/json',
           now(),
           NULL,
           NULL,
           7,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 30.00000, 500.00000, 0.01000);

-- Операция 3
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Зарегистрировать инцидент',
           'Регистрация инцидентов при бурении с указанием типа, времени и последствий.',
           'COMMAND',
           'application/json',
           now(),
           NULL,
           NULL,
           7,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 2.00000, 2000.00000, 0.05000);


----------------------------------------------------------------
-- Продукт 3: Платформа управления контрактами
----------------------------------------------------------------
INSERT INTO product.product (
    name, alias, description, git_url,
    structurizr_workspace_name, structurizr_api_key, structurizr_api_secret,
    source, upload_date, critical
)
VALUES (
           'Платформа управления контрактами',
           'contract-clm',
           'Управление жизненным циклом договоров на услуги, поставки и раздел продукции.',
           'https://git.example.com/contracts/clm.git',
           'contract-clm-workspace',
           'contract-clm-key',
           'contract-clm-secret',
           'migration',
           now(),
           'High'
       )
    RETURNING id INTO v_product_id;

INSERT INTO product.containers_product (
    name, product_id, code, version,
    created_date, updated_date, deleted_date
)
VALUES (
           'Contract CLM API',
           v_product_id,
           'contract-clm-api',
           '1.0.0',
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_container_id;

-- Привязываем к технической возможности: TC.00010 (Управление контрактами (CLM), id = 10)
INSERT INTO product.interface (
    name, code, spec_link, version,
    type_id, description, protocol, status_id,
    tc_id, container_id,
    created_date, deleted_date, updated_date
)
VALUES (
           'Contract Management API',
           'CONTRACT-CLM-API',
           'https://api.example.com/contracts/openapi',
           'v1',
           NULL,
           'REST API для регистрации, согласования и мониторинга договоров.',
           'HTTPS/REST',
           NULL,
           10,
           v_container_id,
           now(),
           NULL,
           NULL
       )
    RETURNING id INTO v_interface_id;

-- Операция 1
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Создать договор',
           'Создание карточки договора с основными параметрами и участниками.',
           'COMMAND',
           'application/json',
           now(),
           NULL,
           NULL,
           10,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 5.00000, 1200.00000, 0.02000);

-- Операция 2
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Получить статус договора',
           'Возвращает актуальный статус договора, этап согласования и ключевые даты.',
           'QUERY',
           'application/json',
           now(),
           NULL,
           NULL,
           10,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 25.00000, 700.00000, 0.01000);

-- Операция 3
INSERT INTO product.operation (
    interface_id, name, description, type, return_type,
    created_date, updated_date, deleted_date,
    tc_id, is_deleted_tc
)
VALUES (
           v_interface_id,
           'Зарегистрировать исполнение обязательства',
           'Фиксация факта исполнения договорного обязательства (поставка, услуга и т.п.).',
           'COMMAND',
           'application/json',
           now(),
           NULL,
           NULL,
           10,
           false
       )
    RETURNING id INTO v_operation_id;

INSERT INTO product.sla (operation_id, rps, latency, error_rate)
VALUES (v_operation_id, 15.00000, 900.00000, 0.01500);

END IF;
END $$;

