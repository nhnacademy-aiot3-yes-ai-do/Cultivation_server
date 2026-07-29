package site.yesaido.cultivation_server.dto.cultivationmember.request;

import jakarta.validation.constraints.NotNull;

public record OwnerTransferRequest(
        @NotNull Long userId
) {
}
