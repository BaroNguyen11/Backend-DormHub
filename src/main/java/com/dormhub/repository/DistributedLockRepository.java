package com.dormhub.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Khóa phân tán PostgreSQL (advisory lock) cho các scheduler chạy đa instance.
 *
 * Phiên bản cũ lấy lock bằng {@code pg_try_advisory_lock} trong MỘT transaction
 * (connection A) rồi nhả lock trong transaction khác (connection B) — session-level
 * advisory lock gắn với connection A, do đó lock không bao giờ được nhả đúng cách:
 * billing có thể chết vĩnh viễn hoặc chạy trùng giữa các instance (P0).
 *
 * Bản sửa: lấy connection RIÊNG từ pool, thực hiện cặp
 * try_advisory_lock → chạy công việc → advisory_unlock trên ĐÚNG connection đó,
 * rồi mới đóng. Nếu JVM chết giữa chừng, session Postgres bị hủy và Postgres
 * tự nhả mọi advisory lock của session — không còn kẹt lock vĩnh viễn.
 */
@Repository
public class DistributedLockRepository {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockRepository.class);

    private final DataSource dataSource;

    public DistributedLockRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Chạy {@code action} trong lúc giữ khóa phân tán {@code lockKey}.
     *
     * @return true nếu đã giữ được khóa và chạy xong công việc,
     *         false nếu instance khác đang giữ khóa (bỏ qua lượt này).
     */
    public boolean runWithLock(long lockKey, Runnable action) {
        try (Connection connection = dataSource.getConnection()) {
            if (!trySessionAdvisoryLock(connection, lockKey)) {
                return false;
            }
            try {
                action.run();
                return true;
            } finally {
                unlockSessionAdvisoryLockQuietly(connection, lockKey);
            }
        } catch (SQLException e) {
            log.warn("Không thể mở kết nối để lấy khóa phân tán {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    private boolean trySessionAdvisoryLock(Connection connection, long lockKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            statement.setLong(1, lockKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getBoolean(1)) {
                    return true;
                }
                log.info("Bỏ qua tác vụ: một instance khác đang giữ khóa phân tán {}", lockKey);
                return false;
            }
        }
    }

    private void unlockSessionAdvisoryLockQuietly(Connection connection, long lockKey) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, lockKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !resultSet.getBoolean(1)) {
                    log.warn("Khóa phân tán {} không còn trên connection hiện tại khi nhả", lockKey);
                }
            }
        } catch (SQLException e) {
            log.warn("Không nhả được khóa phân tán {}: {}", lockKey, e.getMessage());
        }
    }
}
