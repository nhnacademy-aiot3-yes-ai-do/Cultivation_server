package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorSnapshotResponse;

// Data Generator 초기 복구용 전체 snapshot 조회 계약입니다.
public interface DataGeneratorSnapshotService {

    DataGeneratorSnapshotResponse getSnapshot();
}
