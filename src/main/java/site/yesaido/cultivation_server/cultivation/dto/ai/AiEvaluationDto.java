package site.yesaido.cultivation_server.cultivation.dto.ai;

public record AiEvaluationDto(
        int difficultyLevel,       // 초보자 난이도 (1: 매우 쉬움 ~ 5: 매우 어려움)
        int growthSpeed,           // 성장 속도 (1: 매우 느림 ~ 5: 수확이 아주 빠름)
        String sensitivity,        // 가장 주의해야 할 환경 요인 (예: "건조함에 매우 취약")
        String aiStrategy          // AI의 1:1 맞춤형 재배 컨설팅 (3문장)
) {
    public AiEvaluationDto{
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("difficultyLevel은 1~5 사이여야 합니다. 입력값: " + difficultyLevel);
        }
        if (growthSpeed < 1 || growthSpeed > 5) {
            throw new IllegalArgumentException("growthSpeed는 1~5 사이여야 합니다. 입력값: " + growthSpeed);
        }
    }
}
