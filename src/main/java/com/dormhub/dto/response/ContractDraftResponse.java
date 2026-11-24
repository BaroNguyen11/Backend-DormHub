package com.dormhub.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractDraftResponse {
    private final String title;
    private final String organizationNote;
    private final ContractLandlordResponse landlord;
    private final String introduction;
    private final StudentProfileResponse student;
    private final RoomResponse room;
    private final int durationMonths;
    private final BigDecimal monthlyRent;
    private final BigDecimal depositAmount;
    private final List<Section> sections;
    private final String signatureNote;

    @Getter
    public static class Section {
        private final String heading;
        private final List<String> paragraphs;

        public Section(String heading, List<String> paragraphs) {
            this.heading = heading;
            this.paragraphs = List.copyOf(paragraphs);
        }
    }
}
