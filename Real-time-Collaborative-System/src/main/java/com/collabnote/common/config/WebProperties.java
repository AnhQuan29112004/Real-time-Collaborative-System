package com.collabnote.common.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("collabnote.web")
public record WebProperties(@DefaultValue Cors cors) {
    public record Cors(@DefaultValue List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
            for (String origin : allowedOrigins) {
                URI uri = URI.create(origin);
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                        || uri.getHost() == null || uri.getRawUserInfo() != null
                        || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                        || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                    throw new IllegalArgumentException("CORS requires explicit HTTP(S) origins without paths");
                }
            }
        }
    }
}
