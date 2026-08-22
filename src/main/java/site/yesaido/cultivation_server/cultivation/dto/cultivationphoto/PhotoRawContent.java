package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

public record PhotoRawContent(
        byte[] bytes,
        String contentType
) {
}
