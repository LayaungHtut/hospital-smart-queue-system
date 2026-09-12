package com.hospitalqueue.repository;

import com.hospitalqueue.model.ChatbotConversation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ChatbotConversationRepository {

    private final JdbcTemplate jdbc;

    public ChatbotConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ChatbotConversation> MAPPER = new RowMapper<>() {
        @Override
        public ChatbotConversation mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChatbotConversation c = new ChatbotConversation();
            c.setConversationId(rs.getLong("conversation_id"));
            c.setSessionId(rs.getString("session_id"));
            c.setUserType(rs.getString("user_type"));
            c.setUserId(rs.getString("user_id"));
            c.setMessageRole(rs.getString("message_role"));
            c.setMessageContent(rs.getString("message_content"));
            c.setRetrievedChunks(rs.getString("retrieved_chunks"));
            c.setResponseTimeMs(rs.getInt("response_time_ms"));
            c.setHelpful(rs.getBoolean("helpful"));
            c.setFeedbackText(rs.getString("feedback_text"));
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return c;
        }
    };

    public ChatbotConversation save(ChatbotConversation conversation) {
        String sql = """
            INSERT INTO chatbot_conversation (session_id, user_type, user_id, message_role, 
                                              message_content, retrieved_chunks, response_time_ms)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
            RETURNING conversation_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                conversation.getSessionId(), conversation.getUserType(), conversation.getUserId(),
                conversation.getMessageRole(), conversation.getMessageContent(),
                conversation.getRetrievedChunks(), conversation.getResponseTimeMs());
        conversation.setConversationId(id);
        conversation.setCreatedAt(LocalDateTime.now());
        return conversation;
    }

    public void updateFeedback(Long conversationId, boolean helpful, String feedbackText) {
        String sql = "UPDATE chatbot_conversation SET helpful = ?, feedback_text = ? WHERE conversation_id = ?";
        jdbc.update(sql, helpful, feedbackText, conversationId);
    }

    public List<ChatbotConversation> findBySessionId(String sessionId) {
        String sql = "SELECT * FROM chatbot_conversation WHERE session_id = ? ORDER BY created_at";
        return jdbc.query(sql, MAPPER, sessionId);
    }

    public List<ChatbotConversation> findByUser(String userType, String userId) {
        String sql = "SELECT * FROM chatbot_conversation WHERE user_type = ? AND user_id = ? ORDER BY created_at DESC LIMIT 50";
        return jdbc.query(sql, MAPPER, userType, userId);
    }
}