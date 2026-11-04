package com.dormhub.service;

import com.dormhub.dto.request.ResidenceTermRequest;
import com.dormhub.dto.response.ResidenceTermResponse;
import com.dormhub.entity.ResidenceTerm;
import com.dormhub.entity.enums.ResidenceTermStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.ResidenceTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResidenceTermService {

    private final ResidenceTermRepository termRepository;

    @Transactional(readOnly = true)
    public ResidenceTermResponse getActiveTerm() {
        ResidenceTerm term = termRepository.findFirstByStatus(ResidenceTermStatus.OPEN)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Hiện tại không có đợt đăng ký nào đang mở"));
        return mapToResponse(term);
    }

    @Transactional(readOnly = true)
    public Page<ResidenceTermResponse> getAllTerms(Pageable pageable) {
        return termRepository.findAll(pageable).map(this::mapToResponse);
    }
    @Transactional(readOnly = true)
    public ResidenceTermResponse getTermById(Long id) {
        ResidenceTerm term = termRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy đợt đăng ký"));
        return mapToResponse(term);
    }

    @Transactional
    public ResidenceTermResponse createTerm(ResidenceTermRequest request) {
        validateDateRange(request);

        ResidenceTermStatus status = request.getStatus() != null ? request.getStatus() : ResidenceTermStatus.DRAFT;

        if (status == ResidenceTermStatus.OPEN && termRepository.existsByStatusAndIdNot(ResidenceTermStatus.OPEN, 0L)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đã có một đợt đăng ký khác đang ở trạng thái OPEN. Hãy đóng đợt cũ trước!");
        }

        ResidenceTerm term = ResidenceTerm.builder()
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registrationStartDate(request.getRegistrationStartDate())
                .registrationEndDate(request.getRegistrationEndDate())
                .status(status)
                .build();

        return mapToResponse(termRepository.save(term));
    }

    @Transactional
    public ResidenceTermResponse updateTerm(Long id, ResidenceTermRequest request) {
        validateDateRange(request);

        ResidenceTerm term = termRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy đợt đăng ký"));

        term.setName(request.getName().trim());
        term.setStartDate(request.getStartDate());
        term.setEndDate(request.getEndDate());
        term.setRegistrationStartDate(request.getRegistrationStartDate());
        term.setRegistrationEndDate(request.getRegistrationEndDate());
        if (request.getStatus() != null) {
            term.setStatus(request.getStatus());
        }

        return mapToResponse(termRepository.save(term));
    }

    @Transactional
    public ResidenceTermResponse updateStatus(Long id, ResidenceTermStatus status) {
        ResidenceTerm term = termRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy đợt đăng ký"));

        if (status == ResidenceTermStatus.OPEN && termRepository.existsByStatusAndIdNot(ResidenceTermStatus.OPEN, id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đang có một đợt khác OPEN. Vui lòng đóng đợt cũ trước khi mở đợt này!");
        }

        term.setStatus(status);
        return mapToResponse(termRepository.save(term));
    }

    private void validateDateRange(ResidenceTermRequest req) {
        if (req.getRegistrationStartDate().isAfter(req.getRegistrationEndDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày bắt đầu nhận đơn phải trước ngày đóng nhận đơn");
        }
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày bắt đầu lưu trú phải trước ngày kết thúc lưu trú");
        }
    }

    private ResidenceTermResponse mapToResponse(ResidenceTerm entity) {
        return ResidenceTermResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .registrationStartDate(entity.getRegistrationStartDate())
                .registrationEndDate(entity.getRegistrationEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}