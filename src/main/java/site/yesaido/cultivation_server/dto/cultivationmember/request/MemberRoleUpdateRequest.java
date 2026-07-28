package site.yesaido.cultivation_server.dto.cultivationmember.request;

import jakarta.validation.constraints.NotNull;
import site.yesaido.cultivation_server.entity.cultivationmember.MemberRole;

public record MemberRoleUpdateRequest(
        @NotNull MemberRole role
) {
}
