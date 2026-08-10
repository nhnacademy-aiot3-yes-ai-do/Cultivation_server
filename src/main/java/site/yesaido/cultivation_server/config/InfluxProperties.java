package site.yesaido.cultivation_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influx")
public class InfluxProperties {

    private String url;
    private String org;
    private String bucket;
    private String token;
    private final Cloudflare cloudflare = new Cloudflare();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Cloudflare getCloudflare() {
        return cloudflare;
    }

    public static class Cloudflare {
        private String accessClientId;
        private String accessClientSecret;

        public String getAccessClientId() {
            return accessClientId;
        }

        public void setAccessClientId(String accessClientId) {
            this.accessClientId = accessClientId;
        }

        public String getAccessClientSecret() {
            return accessClientSecret;
        }

        public void setAccessClientSecret(String accessClientSecret) {
            this.accessClientSecret = accessClientSecret;
        }

        public boolean isConfigured() {
            return hasText(accessClientId) && hasText(accessClientSecret);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
