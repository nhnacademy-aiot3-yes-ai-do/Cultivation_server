package site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request;

import jakarta.validation.constraints.NotNull;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;

public record MemberRoleUpdateRequest(
        @NotNull MemberRole role
) {
}
