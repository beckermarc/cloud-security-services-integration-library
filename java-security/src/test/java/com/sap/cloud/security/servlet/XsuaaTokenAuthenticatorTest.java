package com.sap.cloud.security.servlet;

import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants;
import com.sap.cloud.security.token.SecurityContext;
import com.sap.cloud.security.token.XsuaaToken;
import com.sap.cloud.security.token.validation.ValidationListener;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenKeyService;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenKeyServiceWithCache;
import com.sap.cloud.security.xsuaa.http.HttpHeaders;
import com.sap.cloud.security.xsuaa.jwk.JsonWebKeySetFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class XsuaaTokenAuthenticatorTest {

	private final static HttpServletResponse HTTP_RESPONSE = Mockito.mock(HttpServletResponse.class);

	private final XsuaaToken xsuaaToken;
	private final XsuaaToken invalidSignatureToken;

	private AbstractTokenAuthenticator cut;

	public XsuaaTokenAuthenticatorTest() throws IOException {
		xsuaaToken = new XsuaaToken(IOUtils.resourceToString("/xsuaaUserAccessTokenRSA256.txt", UTF_8));
		invalidSignatureToken = new XsuaaToken(
				IOUtils.resourceToString("/xsuaaInvalidSignatureTokenRSA256.txt", UTF_8));
	}

	@Before
	public void setUp() throws IOException {
		OAuth2TokenKeyService tokenKeyService = Mockito.mock(OAuth2TokenKeyService.class);
		when(tokenKeyService.retrieveTokenKeys(any())).thenReturn(
				JsonWebKeySetFactory.createFromJson(IOUtils.resourceToString("/jsonWebTokenKeys.json", UTF_8)));
		OAuth2ServiceConfiguration oAuth2ServiceConfiguration = OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA)
				.withProperty(CFConstants.XSUAA.UAA_DOMAIN, "auth.com")
				.withProperty(CFConstants.XSUAA.APP_ID, "appId")
				.withClientId("clientId")
				.build();

		cut = new XsuaaTokenAuthenticator()
				.withOAuth2TokenKeyService(
						OAuth2TokenKeyServiceWithCache.getInstance().withTokenKeyService(tokenKeyService))
				.withServiceConfiguration(oAuth2ServiceConfiguration);
	}

	@Test
	public void validateRequest_noHeader_isUnauthenticated() {
		HttpServletRequest httpRequest = createRequestWithoutToken();

		TokenAuthenticationResult response = cut.validateRequest(httpRequest, HTTP_RESPONSE);

		assertThat(response.isAuthenticated()).isFalse();
		assertThat(response.getUnauthenticatedReason()).contains("Authorization header is missing");
	}

	@Test
	public void validateRequest_invalidToken_isUnauthenticated() {
		String errorMessage = "JWT token does not consist of 'header'.'payload'.'signature'";
		HttpServletRequest httpRequest = createRequestWithToken("Bearer invalid");

		TokenAuthenticationResult response = cut.validateRequest(httpRequest, HTTP_RESPONSE);

		assertThat(response.isAuthenticated()).isFalse();
		assertThat(response.getUnauthenticatedReason()).contains(errorMessage);
	}

	@Test
	public void validateRequest_validToken_containedInSecurityContext() {
		HttpServletRequest httpRequest = createRequestWithToken(validToken());

		TokenAuthenticationResult response = cut.validateRequest(httpRequest, HTTP_RESPONSE);

		assertThat(response.getUnauthenticatedReason()).isEmpty();
		assertThat(response.isAuthenticated()).isTrue();
		assertThat(response.getToken()).isSameAs(SecurityContext.getToken());
	}

	@Test
	public void validateRequest_validToken_listenerIsCalled() {
		HttpServletRequest httpRequest = createRequestWithToken(validToken());
		ValidationListener validationListener = Mockito.mock(ValidationListener.class);

		cut.withValidationListener(validationListener).validateRequest(httpRequest, HTTP_RESPONSE);

		Mockito.verify(validationListener, times(1)).onValidationSuccess();
		Mockito.verifyNoMoreInteractions(validationListener);
	}

	@Test
	public void validateRequest_invalidToken_listenerIsCalled() {
		HttpServletRequest httpRequest = createRequestWithToken(invalidToken());
		ValidationListener validationListener = Mockito.mock(ValidationListener.class);

		TokenAuthenticationResult resp = cut.withValidationListener(validationListener)
				.validateRequest(httpRequest, HTTP_RESPONSE);

		Mockito.verify(validationListener, times(1)).onValidationError(any());
		Mockito.verifyNoMoreInteractions(validationListener);
	}

	private String invalidToken() {
		return invalidSignatureToken.getBearerAccessToken();
	}

	private String validToken() {
		return xsuaaToken.getBearerAccessToken();
	}

	private HttpServletRequest createRequestWithoutToken() {
		return Mockito.mock(HttpServletRequest.class);
	}

	private HttpServletRequest createRequestWithToken(String bearerAuthorizationHeader) {
		HttpServletRequest httpRequest = createRequestWithoutToken();
		when(httpRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(bearerAuthorizationHeader);
		return httpRequest;
	}

}
