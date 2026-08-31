package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import site.yesaido.cultivation_server.sensor.support.SensorUnits;

public record SensorTypeRequest(
        @NotBlank
        @Size(max = 32)
        String type,

        @NotBlank
        @Size(max = 16)
        String valueUnit
        // 가능 단위 예시
        /*
                %
                kPa
                pF
                Lux
                W/m²
                dS/m
                mS/cm
                ppm
                mg/m³
                m/s
                km/h
                Knots
                µmol/m²·s
                °C
                °F
         */
) {
        public SensorTypeRequest {
                valueUnit = SensorUnits.normalize(valueUnit);
        }
}
