package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import java.util.Arrays;
import java.util.Objects;

public record PhotoRawContent(
        byte[] bytes,
        String contentType
) {
    // byte[]는 record가 자동 생성하는 equals/hashCode가 내용이 아니라 참조를 비교해서 직접 오버라이드해야 함.
    // toString도 기본 생성은 [B@해시코드처럼 의미 없는 값이 나와서, 배열 내용을 그대로 덤프하는 대신 길이만 보여주는 게 실용적임

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoRawContent that)) return false;
        return Arrays.equals(bytes, that.bytes) && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(bytes), contentType);
    }

    @Override
    public String toString() {
        return "PhotoRawContent[bytes.length=" + (bytes != null ? bytes.length : 0) + ", contentType=" + contentType + "]";
    }
}
