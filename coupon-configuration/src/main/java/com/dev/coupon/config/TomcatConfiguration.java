package com.dev.coupon.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfiguration {

	private static final int MAX_LOCALE_COUNT = 50;

	@Bean
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> maxLocaleCountCustomizer() {
		return factory -> factory.addConnectorCustomizers(
				connector -> connector.setMaxLocaleCount(MAX_LOCALE_COUNT)
		);
	}
}
