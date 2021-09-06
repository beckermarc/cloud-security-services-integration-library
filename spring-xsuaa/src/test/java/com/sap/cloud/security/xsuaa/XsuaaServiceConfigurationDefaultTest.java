package com.sap.cloud.security.xsuaa;

import com.sap.cloud.security.config.cf.CFConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sap.cloud.security.config.cf.CFConstants.VCAP_SERVICES;
import static com.sap.cloud.security.config.cf.CFConstants.XSUAA.*;
import static org.assertj.core.api.Assertions.assertThat;

public class XsuaaServiceConfigurationDefaultTest {
	XsuaaServiceConfigurationDefault cut;

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@Before
	public void setup() {
		environmentVariables.set(VCAP_SERVICES,
				"{\"xsuaa\":[{\"credentials\":{\"apiurl\":\"https://api.mydomain.com\",\"tenantid\":\"tenant-id\",\"subaccountid\":\"subaccount-id\",\"clientid\":\"client-id\"},\"tags\":[\"xsuaa\"]}]}");
		cut = new XsuaaServiceConfigurationDefault();
	}

	@Test
	public void getProperty() {
		assertThat(cut.getProperty(API_URL)).isEqualTo("https://api.mydomain.com");
		assertThat(cut.getProperty(SUBACCOUNT_ID)).isEqualTo("subaccount-id");
		assertThat(cut.getProperty(TENANT_ID)).isEqualTo("tenant-id");
		assertThat(cut.getProperty(CFConstants.CLIENT_ID)).isEqualTo("client-id");
		assertThat(cut.getProperty("unknownProp")).isNull();
	}

	@Test
	public void hasProperty() {
		assertThat(cut.hasProperty(API_URL)).isTrue();
		assertThat(cut.hasProperty(SUBACCOUNT_ID)).isTrue();
		assertThat(cut.hasProperty("unknownProp")).isFalse();
	}
}