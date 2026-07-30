package site.yesaido.cultivation_server.cultivation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import site.yesaido.cultivation_server.cultivation.dto.user.UserSummaryResponse;

import java.util.List;

@FeignClient(name = "user-server")
public interface UserClient {
    @GetMapping("/users/batch")
    List<UserSummaryResponse> getUsers(@RequestParam("ids") List<Long> ids);
}
