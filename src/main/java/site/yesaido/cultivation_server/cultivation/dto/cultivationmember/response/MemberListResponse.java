package site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response;

import java.util.List;

public record MemberListResponse(
        List<MemberResponse> memberResponses
) {
}
