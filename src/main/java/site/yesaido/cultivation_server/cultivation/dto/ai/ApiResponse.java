package site.yesaido.cultivation_server.cultivation.dto.ai;

public record ApiResponse<T>(
        boolean success, // 성공/실패 여부
        String message, // 메시지
        T data // 데이터
) {
    public static <T> ApiResponse<T> success(T data){ // 성공했을 때 호출할 메서드(데이터 포함)
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data);
    }
}
