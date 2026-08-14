package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorSnapshotResponse;
import site.yesaido.cultivation_server.sensor.service.DataGeneratorSnapshotService;

// Data Generator의 초기 복구용 내부 snapshot API를 제공합니다.
@RestController
@RequestMapping("/api/internal/data-generator")
@RequiredArgsConstructor
public class DataGeneratorSnapshotController {

    private final DataGeneratorSnapshotService dataGeneratorSnapshotService;

    @GetMapping("/snapshot")
    public ResponseEntity<DataGeneratorSnapshotResponse> getSnapshot() {
        return ResponseEntity.ok(
                dataGeneratorSnapshotService.getSnapshot()
        );
    }
}