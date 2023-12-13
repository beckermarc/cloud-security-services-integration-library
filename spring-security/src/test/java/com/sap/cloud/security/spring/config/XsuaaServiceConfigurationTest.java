/**
 * SPDX-FileCopyrightText: 2018-2023 SAP SE or an SAP affiliate company and Cloud Security Client Java contributors
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sap.cloud.security.spring.config;

import com.sap.cloud.environment.servicebinding.SapVcapServicesServiceBindingAccessor;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.ServiceBindingEnvironment;
import com.sap.cloud.security.config.ServiceConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = { SingleXsuaaConfigurationFromFile.class })
class XsuaaServiceConfigurationTest {

	static ServiceBindingEnvironment env;

	@Autowired
	XsuaaServiceConfiguration xsuaaConfig;

	@BeforeAll
	static void setup() throws IOException {
		String serviceBindingJson = IOUtils.resourceToString("/singleXsuaaAndIasBinding.json", UTF_8);
		env = new ServiceBindingEnvironment(new SapVcapServicesServiceBindingAccessor(any -> serviceBindingJson));
	}

	@Test
	void configuresXsuaaServiceConfiguration() {
		assertConfigsAreEqual(xsuaaConfig, env.getXsuaaConfiguration());
    }

	static void assertConfigsAreEqual(XsuaaServiceConfiguration xsuaaConfig, OAuth2ServiceConfiguration oauthConfig) {
		assertEquals(oauthConfig.getClientId(), xsuaaConfig.getClientId());
		assertEquals(oauthConfig.getClientSecret(), xsuaaConfig.getClientSecret());
		assertEquals(oauthConfig.getProperty(ServiceConstants.XSUAA.UAA_DOMAIN), xsuaaConfig.getProperty(ServiceConstants.XSUAA.UAA_DOMAIN));
		assertEquals(oauthConfig.getProperty(ServiceConstants.XSUAA.APP_ID), xsuaaConfig.getProperty(ServiceConstants.XSUAA.APP_ID));
		assertEquals(oauthConfig.getProperty(ServiceConstants.SERVICE_PLAN), xsuaaConfig.getPlan());
	}
}

@Configuration
@PropertySource(factory = IdentityServicesPropertySourceFactory.class, value = { "classpath:singleXsuaaAndIasBinding.json" })
@EnableConfigurationProperties(XsuaaServiceConfiguration.class)
class SingleXsuaaConfigurationFromFile {}
