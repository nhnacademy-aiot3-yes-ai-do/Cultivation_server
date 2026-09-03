package site.yesaido.cultivation_server.sensor.dto.projection;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;

import java.time.LocalDateTime;

public interface CultivationSummaryProjection {
    Long getCultivationId();
    String getName();
    Long getMushroomId();
    CultivationStatus getStatus();
    CultivationMode getMode();
    Long getMemberCount();
    Long getOwnerUserId();
    MemberRole getMyRole();
    LocalDateTime getStartedAt();
    LocalDateTime getFinishedAt();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
