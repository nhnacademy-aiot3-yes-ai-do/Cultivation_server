package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;

public interface MushroomReferenceService {
    long registerMushroomReference(MushroomReferenceRequest dto);
    void updateMushroomReference(long mushroomReferenceId, MushroomReferenceRequest dto);
    void deleteMushroomReference(long mushroomReferenceId);
    MushroomReferenceInfoResponse getMushroomReferenceInfo(long mushroomReferenceId);
    MushroomReferenceInfoListResponse getAllMushroomReferenceInfoList();
}
