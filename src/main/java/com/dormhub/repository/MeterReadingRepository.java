package com.dormhub.repository;

import com.dormhub.entity.MeterReading;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    @Query("select m from MeterReading m where m.readingId > :afterId "
            + "and m.status = com.dormhub.entity.enums.ReadingStatus.CONFIRMED and m.readingMonth < :currentMonth "
            + "and not exists (select i.invoiceId from Invoice i where i.room.id=m.room.id "
            + "and i.billingMonth=m.readingMonth and i.invoiceType=com.dormhub.entity.enums.InvoiceType.UTILITY) "
            + "order by m.readingId")
    @EntityGraph(attributePaths = "room")
    List<MeterReading> findUnbilledConfirmedAfter(@Param("afterId") long afterId,
            @Param("currentMonth") YearMonth currentMonth, Pageable pageable);

    MeterReading findByRoom_IdAndReadingMonth(Long roomId, YearMonth readingMonth);
}
