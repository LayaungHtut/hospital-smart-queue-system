package com.hospitalqueue.repository;

import com.hospitalqueue.model.Symptom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

@Repository
public class SymptomRepository {

    private final JdbcTemplate jdbcTemplate;

    public SymptomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Symptom> mapper = (RowMapper<Symptom>) (ResultSet rs, int rowNum) -> {
        Symptom s = new Symptom();
        s.setSymptomId(rs.getInt("symptom_id"));
        s.setSymptomName(rs.getString("symptom_name"));
        s.setSymptomCode(rs.getString("symptom_code"));
        s.setCategory(rs.getString("category"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    };

    public List<Symptom> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM symptom WHERE is_active = TRUE ORDER BY category, symptom_name",
                mapper);
    }

    public Symptom findById(int symptomId) {
        List<Symptom> list = jdbcTemplate.query(
                "SELECT * FROM symptom WHERE symptom_id = ?", mapper, symptomId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Symptom findByCode(String code) {
        List<Symptom> list = jdbcTemplate.query(
                "SELECT * FROM symptom WHERE symptom_code = ?", mapper, code);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Symptom> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();
        String inSql = String.join(",", ids.stream().map(String::valueOf).toList());
        return jdbcTemplate.query(
                "SELECT * FROM symptom WHERE symptom_id IN (" + inSql + ") AND is_active = TRUE ORDER BY symptom_name",
                mapper);
    }

    public void linkQueueSymptoms(long queueId, List<Integer> symptomIds) {
        if (symptomIds == null || symptomIds.isEmpty())
            return;
        for (Integer symptomId : symptomIds) {
            jdbcTemplate.update(
                    "INSERT INTO queue_symptom (queue_id, symptom_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    queueId, symptomId);
        }
    }

    public List<Symptom> findByQueueId(long queueId) {
        return jdbcTemplate.query(
                "SELECT s.* FROM symptom s JOIN queue_symptom qs ON s.symptom_id = qs.symptom_id WHERE qs.queue_id = ? ORDER BY s.symptom_name",
                mapper, queueId);
    }

    public String getSymptomNamesForQueue(long queueId) {
        List<Symptom> symptoms = findByQueueId(queueId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < symptoms.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(symptoms.get(i).getSymptomName());
        }
        return sb.toString();
    }
}
