package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.harvest.Harvest;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;
import site.yesaido.cultivation_server.cultivation.exception.*;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.repository.harvest.HarvestRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.cultivation.service.HarvestService;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedEvent;
import site.yesaido.cultivation_server.rabbitmq.event.HarvestCompletedPayload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HarvestServiceImpl implements HarvestService {
    private final HarvestRepository harvestRepository;
    private final CultivationRepository cultivationRepository;
    private final CultivationMemberService cultivationMemberService;
    private final ApplicationEventPublisher eventPublisher;
    private final CultivationAccessGuard cultivationAccessGuard;

    @Override
    @Transactional
    public HarvestCreateResponse createHarvest(Long cultivationId, Long userId, HarvestCreateRequest harvestCreateRequest) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId);
        if (cultivation.getCultivationStatus() == CultivationStatus.FINISHED) {
            throw new CultivationAlreadyFinishedException(cultivationId);
        }
        if (harvestRepository.existsByCultivationId(cultivationId)) {
            throw new HarvestAlreadyExistException(cultivationId);
        }
        Harvest harvest = Harvest.builder()
                .harvestWeight(harvestCreateRequest.harvestWeight())
                .memo(harvestCreateRequest.memo())
                .harvestedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .cultivation(cultivation)
                .build();
        harvestRepository.save(harvest);
        cultivation.finish();

        HarvestCompletedPayload payload = new HarvestCompletedPayload(cultivation.getName(), harvest.getHarvestWeight());
        eventPublisher.publishEvent(new HarvestCompletedEvent(cultivationId, payload));

        return new HarvestCreateResponse(
                harvest.getId(),
                harvest.getHarvestWeight(),
                harvest.getHarvestedAt(),
                harvest.getProductScore(),
                harvest.getProductGrade()
        );
    }

    @Override
    public HarvestDetailResponse getHarvest(Long cultivationId, Long userId) {
        Cultivation cultivation = cultivationAccessGuard.requireMember(cultivationId, userId);

        Harvest harvest = harvestRepository.findByCultivationId(cultivationId)
                .orElseThrow(() -> new HarvestNotFoundException(cultivationId));

        return new HarvestDetailResponse(
                harvest.getId(),
                cultivationId,
                harvest.getHarvestWeight(),
                cultivation.getName(),
                harvest.getHarvestedAt(),
                harvest.getProductScore(),
                harvest.getProductGrade()
        );
    }

    @Override
    @Transactional
    public ProductScoreUpdateResponse updateProductScore(Long cultivationId, Long userId, ProductScoreUpdateRequest request) {

        if (!cultivationRepository.existsById(cultivationId)) {
            throw new CultivationNotFoundException(cultivationId);
        }

        cultivationMemberService.verifyOwnerAccess(cultivationId, userId);

        Harvest harvest = harvestRepository.findByCultivationId(cultivationId)
                .orElseThrow(() -> new HarvestNotFoundException(cultivationId));

        ProductGrade grade = toGrade(request.productScore());
        harvest.updateProductGrade(request.productScore(), grade);

        return new ProductScoreUpdateResponse(harvest.getId(), harvest.getProductScore(), harvest.getProductGrade());
    }

    // Helper Method
    private ProductGrade toGrade(BigDecimal productScore) {
        if (productScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return ProductGrade.TOP;
        }
        if (productScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return ProductGrade.HIGH;
        }
        if (productScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return ProductGrade.MID;
        }
        return ProductGrade.LOW;
    }
}
