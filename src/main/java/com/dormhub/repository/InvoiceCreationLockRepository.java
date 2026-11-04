package com.dormhub.repository;

import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceCreationLockRepository {

    private final EntityManager entityManager;

    public InvoiceCreationLockRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Room lockRoom(Long roomId) {
        return entityManager.find(Room.class, roomId, LockModeType.PESSIMISTIC_WRITE);
    }

    public Contract lockContract(Long contractId) {
        return entityManager.find(Contract.class, contractId, LockModeType.PESSIMISTIC_WRITE);
    }
}
