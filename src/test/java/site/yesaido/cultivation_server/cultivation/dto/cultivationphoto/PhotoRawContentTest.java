package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoRawContentTest {

    @Test
    void equalsAndHashCode_sameContentDifferentArrayInstances_areEqual() {
        PhotoRawContent a = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");
        PhotoRawContent b = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentBytes_areNotEqual() {
        PhotoRawContent a = new PhotoRawContent("image-bytes-1".getBytes(), "image/jpeg");
        PhotoRawContent b = new PhotoRawContent("image-bytes-2".getBytes(), "image/jpeg");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_differentContentType_areNotEqual() {
        PhotoRawContent a = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");
        PhotoRawContent b = new PhotoRawContent("image-bytes".getBytes(), "image/png");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_differentType_returnsFalse() {
        PhotoRawContent a = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");

        assertThat(a).isNotEqualTo("not-a-photo-raw-content");
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        PhotoRawContent a = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");

        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void toString_containsLengthNotRawBytes() {
        PhotoRawContent a = new PhotoRawContent("image-bytes".getBytes(), "image/jpeg");

        assertThat(a.toString())
                .contains("bytes.length=" + "image-bytes".getBytes().length)
                .contains("image/jpeg");
    }
}