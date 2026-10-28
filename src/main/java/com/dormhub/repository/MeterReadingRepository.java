package com.dormhub.repository;

import com.dormhub.entity.MeterReading;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    MeterReading findByRoom_RoomIdAndReadingMonth(Long roomId, YearMonth readingMonth);
}
