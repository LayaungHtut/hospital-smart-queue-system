package com.hospitalqueue.repository;

import com.hospitalqueue.model.MlModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class MlModelRepository {

    private final JdbcTemplate jdbc;

    public MlModelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<MlModel> MAPPER = new RowMapper<>() {
        @Override
        public MlModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            MlModel m = new MlModel();
            m.setModelId(rs.getLong("model_id"));
            m.setModelName(rs.getString("model_name"));
            m.setModelType(rs.getString("model_type"));
            m.setFramework(rs.getString("framework"));
            m.setVersion(rs.getString("version"));
            m.setFilePath(rs.getString("file_path"));
            m.setArtifactsPath(rs.getString("artifacts_path"));
            m.setMetricsJson(rs.getString("metrics_json"));
            m.setActive(rs.getBoolean("is_active"));
            m.setTrainedAt(rs.getTimestamp("trained_at").toLocalDateTime());
            var deployed = rs.getTimestamp("deployed_at");
            m.setDeployedAt(deployed != null ? deployed.toLocalDateTime() : null);
            m.setCreatedBy(rs.getString("created_by"));
            m.setDescription(rs.getString("description"));
            return m;
        }
    };

    public Optional<MlModel> findActiveByType(String modelType) {
        String sql = "SELECT * FROM ml_model WHERE model_type = ? AND is_active = TRUE ORDER BY trained_at DESC LIMIT 1";
        List<MlModel> results = jdbc.query(sql, MAPPER, modelType);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<MlModel> findByType(String modelType) {
        return jdbc.query("SELECT * FROM ml_model WHERE model_type = ? ORDER BY trained_at DESC", MAPPER, modelType);
    }

    public Optional<MlModel> findByName(String modelName) {
        String sql = "SELECT * FROM ml_model WHERE model_name = ?";
        List<MlModel> results = jdbc.query(sql, MAPPER, modelName);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public MlModel save(MlModel model) {
        String sql = """
            INSERT INTO ml_model (model_name, model_type, framework, version, file_path, artifacts_path, 
                                  metrics_json, is_active, trained_at, deployed_at, created_by, description)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
            ON CONFLICT (model_name) DO UPDATE SET
                model_type = EXCLUDED.model_type,
                framework = EXCLUDED.framework,
                version = EXCLUDED.version,
                file_path = EXCLUDED.file_path,
                artifacts_path = EXCLUDED.artifacts_path,
                metrics_json = EXCLUDED.metrics_json,
                is_active = EXCLUDED.is_active,
                deployed_at = EXCLUDED.deployed_at,
                description = EXCLUDED.description
            RETURNING model_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                model.getModelName(), model.getModelType(), model.getFramework(), model.getVersion(),
                model.getFilePath(), model.getArtifactsPath(), model.getMetricsJson(),
                model.isActive(), model.getTrainedAt(), model.getDeployedAt(),
                model.getCreatedBy(), model.getDescription());
        model.setModelId(id);
        return model;
    }

    public void setActive(String modelName, boolean active) {
        jdbc.update("UPDATE ml_model SET is_active = ? WHERE model_name = ?", active, modelName);
    }
}