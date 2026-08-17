package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CultivationHistoryPageResponseTest {

    @Test
    @DisplayName("from() - Page의 필드를 그대로 옮겨 담는다")
    void fromCopiesPageFields() {
        CultivationHistoryResponse content = new CultivationHistoryResponse(
                1L, "버섯 농장", 10L, null, null, null, null);
        PageImpl<CultivationHistoryResponse> page = new PageImpl<>(
                List.of(content), PageRequest.of(0, 20), 1L);

        CultivationHistoryPageResponse response = CultivationHistoryPageResponse.from(page);

        assertThat(response.content()).containsExactly(content);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.number()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
    }
}