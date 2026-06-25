package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "blog.oss")
public class OssProperties {

    private boolean enabled = true;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String cdnDomain;

    /** 是否已配置有效的 OSS 凭证 */
    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(endpoint)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret)
                && StringUtils.hasText(bucketName)
                && !accessKeyId.startsWith("your-");
    }

    /** 生成可公开访问的完整 HTTPS 图片 URL */
    public String resolvePublicUrl(String objectKey) {
        if (StringUtils.hasText(cdnDomain)) {
            String domain = cdnDomain.endsWith("/") ? cdnDomain.substring(0, cdnDomain.length() - 1) : cdnDomain;
            return domain + "/" + objectKey;
        }
        String host = endpoint.startsWith("http") ? endpoint : "https://" + bucketName + "." + endpoint;
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host + "/" + objectKey;
    }
}
