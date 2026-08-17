package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CultivationHistoryPageResponse(
        List<CultivationHistoryResponse> content,
        int totalPages,
        long totalElements,
        int number,
        int size
) {
    public static CultivationHistoryPageResponse from(Page<CultivationHistoryResponse> page) {
        return new CultivationHistoryPageResponse(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }
}
