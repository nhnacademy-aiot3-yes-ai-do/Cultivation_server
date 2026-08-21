package site.yesaido.cultivation_server.cultivation.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import site.yesaido.cultivation_server.cultivation.dto.ai.ApiResponse;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;

@FeignClient(name = "ai-server", url = "${feign.client.ai-server.url}")
public interface AiClient {
    @GetMapping("/api/mushrooms/{mushroom-id}/guide")
    ApiResponse<MushGuideResponse> getMushroomGuide(@PathVariable("mushroom-id") Long mushroomId);
}
