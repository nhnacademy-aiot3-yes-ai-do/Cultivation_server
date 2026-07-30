package site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request;

import jakarta.validation.constraints.NotNull;

public record OwnerTransferRequest(
        @NotNull Long userId
) {
}
