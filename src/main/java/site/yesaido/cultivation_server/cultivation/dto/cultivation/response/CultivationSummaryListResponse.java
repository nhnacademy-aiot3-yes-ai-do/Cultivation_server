package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import java.util.List;

public record CultivationSummaryListResponse(
        List<CultivationSummaryResponse> cultivationSummaryResponses
) {
}
