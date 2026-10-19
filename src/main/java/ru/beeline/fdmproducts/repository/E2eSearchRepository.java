/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;

import java.util.List;

/**
 * Поиск карточек e2e для GET /api/v1/e2e (SFDM-4240) одним запросом. Переданные фильтры дают пересечение,
 * непереданный фильтр выборку не сужает. Корневой метод — вызываемый discovered_operation связи с пустым
 * operation_id. Без tc-code, method и operation-id связи не читаются; без method product.operation не читается.
 */
@Repository
@RequiredArgsConstructor
public class E2eSearchRepository {

    public enum BiStepFilter { ALL, WITHOUT_BI_STEP, WITH_BI_STEP }

    /** Проверенные параметры поиска: null — фильтр не передан. Строки уже после trim. */
    public record Criteria(BiStepFilter filter, String alias, String tcCode, String method, Integer operationId) {
    }

    public record Query(String sql, MapSqlParameterSource params) {
    }

    private static final String SELECT = """
            SELECT e.id, e.code, e.name, e.description, e.bi_step_code, e.source, e.product_id,
                   p.alias AS product_alias
            FROM product.e2e e
            LEFT JOIN product.product p ON p.id = e.product_id
            WHERE 1 = 1""";

    /** SFDM-4250: связь принадлежит ветке e2e, корневая связь ищется во всех ветках карточки. */
    private static final String ROOT_RELATION = """
            SELECT 1 FROM product.operation_relations r
                JOIN product.e2e_branch b ON b.id = r.e2e_branch_id
                JOIN product.discovered_operation d ON d.id = r.related_operation_id""";

    private static final String ROOT_OF_E2E = " WHERE b.code = e.code AND r.operation_id IS NULL";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<E2eCardResponseDTO> search(Criteria criteria) {
        Query query = build(criteria);
        return jdbcTemplate.query(query.sql(), query.params(), (rs, rowNum) -> {
            Integer productId = (Integer) rs.getObject("product_id");
            return E2eCardResponseDTO.builder()
                    .id(rs.getInt("id"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .biStepCode(rs.getString("bi_step_code"))
                    .source(rs.getString("source"))
                    .productId(productId)
                    .productAlias(productId == null ? null : rs.getString("product_alias"))
                    .build();
        });
    }

    public static Query build(Criteria criteria) {
        StringBuilder sql = new StringBuilder(SELECT);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (criteria.filter() == BiStepFilter.WITHOUT_BI_STEP) {
            sql.append("\n  AND (e.bi_step_code IS NULL OR btrim(e.bi_step_code) = '')");
        } else if (criteria.filter() == BiStepFilter.WITH_BI_STEP) {
            sql.append("\n  AND e.bi_step_code IS NOT NULL AND btrim(e.bi_step_code) <> ''");
        }

        if (criteria.alias() != null) {
            sql.append("\n  AND p.id IS NOT NULL AND lower(p.alias) = lower(:alias)");
            params.addValue("alias", criteria.alias());
        }

        if (criteria.tcCode() != null) {
            sql.append("\n  AND EXISTS (").append(ROOT_RELATION).append(ROOT_OF_E2E)
                    .append("\n      AND lower(btrim(d.tc_code)) = lower(:tcCode))");
            params.addValue("tcCode", criteria.tcCode());
        }

        if (criteria.method() != null) {
            sql.append("\n  AND EXISTS (").append(ROOT_RELATION)
                    .append("\n        LEFT JOIN product.operation o ON o.id = d.connection_operation_id")
                    .append(ROOT_OF_E2E)
                    .append("\n      AND (lower(coalesce(d.type, '') || ' ' || coalesce(d.name, '')) = lower(:method)")
                    .append("\n        OR (o.id IS NOT NULL")
                    .append(" AND lower(coalesce(o.type, '') || ' ' || coalesce(o.name, '')) = lower(:method))))");
            params.addValue("method", criteria.method());
        }

        if (criteria.operationId() != null) {
            sql.append("\n  AND EXISTS (").append(ROOT_RELATION).append(ROOT_OF_E2E)
                    .append("\n      AND (d.id = :operationId OR d.connection_operation_id = :operationId))");
            params.addValue("operationId", criteria.operationId());
        }

        sql.append("\nORDER BY e.code, e.id");
        return new Query(sql.toString(), params);
    }
}
