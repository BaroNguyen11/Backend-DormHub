package com.dormhub.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomVectorRepository {
    private static final String TABLE = "public.dormhub_room_vectors";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /** Gửi document tới PgVectorStore để tạo embedding và ghi/cập nhật theo ID. */
    public void upsert(Document document) {
        vectorStore.add(List.of(document));
    }

    /** Tạo embedding cho câu truy vấn và lấy tối đa topK document gần nghĩa. */
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build());
    }

    /** Đếm document vector đang lưu để xác định số ứng viên cần tìm. */
    public int count() {
        return jdbcTemplate.queryForObject("select count(*) from " + TABLE, Integer.class);
    }

    /** Chỉ lấy ID document, phục vụ phát hiện vector mồ côi khi reindex. */
    public List<String> ids() {
        return jdbcTemplate.queryForList("select id::text from " + TABLE, String.class);
    }

    /** Xóa các document vector mồ côi qua Spring AI, không xóa Room. */
    public void delete(List<String> ids) {
        if (!ids.isEmpty()) {
            vectorStore.delete(ids);
        }
    }
}
