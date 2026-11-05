package com.dormhub.repository;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DistributedLockRepository {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockRepository.class);
    private final EntityManager entityManager;

    public DistributedLockRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public boolean tryLock(long lockKey) {
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT pg_try_advisory_lock(:key)")
                    .setParameter("key", lockKey)
                    .getSingleResult();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Không thể lấy lock phân tán với key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean unlock(long lockKey) {
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT pg_advisory_unlock(:key)")
                    .setParameter("key", lockKey)
                    .getSingleResult();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Không thể giải phóng lock phân tán với key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }
}

