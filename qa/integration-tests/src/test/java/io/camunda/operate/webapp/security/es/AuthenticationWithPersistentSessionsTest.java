/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.operate.webapp.security.es;

import static io.camunda.operate.util.CollectionUtil.map;
import static io.camunda.operate.webapp.security.OperateURIs.AUTH_PROFILE;
import static io.camunda.operate.webapp.security.Permission.READ;
import static io.camunda.operate.webapp.security.Permission.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.camunda.operate.entities.UserEntity;
import io.camunda.operate.es.RetryElasticsearchClient;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.indices.OperateWebSessionIndex;
import io.camunda.operate.util.apps.nobeans.TestApplicationWithNoBeans;
import io.camunda.operate.webapp.rest.AuthenticationRestService;
import io.camunda.operate.webapp.rest.dto.UserDto;
import io.camunda.operate.webapp.security.AuthenticationTestable;
import io.camunda.operate.webapp.security.ElasticsearchSessionRepository;
import io.camunda.operate.webapp.security.OperateURIs;
import io.camunda.operate.webapp.security.Permission;
import io.camunda.operate.webapp.security.Role;
import io.camunda.operate.webapp.security.RolePermissionService;
import io.camunda.operate.webapp.security.WebSecurityConfig;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This test tests:
 * * authentication and security of REST API
 * * /api/authentications/user endpoint to get current user
 * * {@link UserStorage} is mocked (integration with ELS is not tested)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {
      TestApplicationWithNoBeans.class,
      OperateProperties.class,
      WebSecurityConfig.class,
      ElasticsearchUserService.class,
      RolePermissionService.class,
      AuthenticationRestService.class,
      ElasticSearchUserDetailsService.class,
      RetryElasticsearchClient.class,ElasticsearchSessionRepository.class, OperateWebSessionIndex.class
  },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "camunda.operate.persistentSessionsEnabled = true",
      "management.endpoints.web.exposure.include = info,prometheus,loggers",
      "server.servlet.session.cookie.name = " + OperateURIs.COOKIE_JSESSIONID
  }
)
@ActiveProfiles({ AUTH_PROFILE, "test"})
public class AuthenticationWithPersistentSessionsTest implements AuthenticationTestable {

  private static final String USERNAME = "demo";
  private static final String PASSWORD = "demo";
  private static final String FIRSTNAME = "Firstname";
  private static final String LASTNAME = "Lastname";

  @Autowired
  private OperateProperties operateProperties;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private PasswordEncoder encoder;

  @MockBean
  private UserStorage userStorage;

  @Before
  public void setUp() {
    UserEntity user = new UserEntity()
        .setUsername(USERNAME)
        .setPassword(encoder.encode(PASSWORD))
        .setRoles(map(List.of(Role.OWNER), Role::name))
        .setFirstname(FIRSTNAME)
        .setLastname(LASTNAME);
    given(userStorage.getByName(USERNAME)).willReturn(user);
  }

  @Test
  public void shouldSetCookieAndCSRFToken() {
    // given
    // when
    ResponseEntity<Void> response = login(USERNAME, PASSWORD);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThatCookiesAreSet(response, operateProperties.isCsrfPreventionEnabled());
  }

  @Test
  public void shouldFailWhileLogin() {
    // when
    ResponseEntity<Void> response = login(USERNAME, String.format("%s%d", PASSWORD, 123));

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThatCookiesAreDeleted(response);
  }

  @Test
  public void shouldResetCookie() {
    // given
    ResponseEntity<Void> loginResponse = login(USERNAME, PASSWORD);

    // assume
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThatCookiesAreSet(loginResponse, operateProperties.isCsrfPreventionEnabled());
    // when
    ResponseEntity<?> logoutResponse = logout(loginResponse);

    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThatCookiesAreDeleted(logoutResponse);
  }


  @Test
  public void shouldReturnCurrentUser() {
    //given authenticated user
    ResponseEntity<Void> loginResponse = login(USERNAME, PASSWORD);

    UserDto userDto = getCurrentUser(loginResponse);
    assertThat(userDto.getUsername()).isEqualTo(USERNAME);
    assertThat(userDto.getFirstname()).isEqualTo(FIRSTNAME);
    assertThat(userDto.getLastname()).isEqualTo(LASTNAME);
    assertThat(userDto.isCanLogout()).isTrue();
    assertThat(userDto.getPermissions()).isEqualTo(List.of(READ, WRITE));
  }

  @Test
  public void testEndpointsNotAccessibleAfterLogout() {
    //when user is logged in
    ResponseEntity<Void> loginResponse = login(USERNAME, PASSWORD);

    //then endpoint are accessible
    ResponseEntity<Object> responseEntity = testRestTemplate.exchange(CURRENT_USER_URL, HttpMethod.GET, prepareRequestWithCookies(loginResponse), Object.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isNotNull();

    //when user logged out
    logout(loginResponse);

    //then endpoint is not accessible
    responseEntity = testRestTemplate.exchange(CURRENT_USER_URL, HttpMethod.GET, prepareRequestWithCookies(loginResponse), Object.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThatCookiesAreDeleted(responseEntity);
  }

  @Test
  public void testCanAccessMetricsEndpoint() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/actuator",String.class);
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(response.getBody()).contains("actuator/info");

    ResponseEntity<String> prometheusResponse = testRestTemplate.getForEntity("/actuator/prometheus",String.class);
    assertThat(prometheusResponse.getStatusCodeValue()).isEqualTo(200);
    assertThat(prometheusResponse.getBody()).contains("# TYPE system_cpu_usage gauge");
  }

  @Test
  public void testCanReadAndWriteLoggersActuatorEndpoint() throws JSONException {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/actuator/loggers/io.camunda.operate",String.class);
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"configuredLevel\":\"DEBUG\"");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request =
        new HttpEntity<>(
            new JSONObject().put("configuredLevel", "TRACE").toString(),
            headers);
    response = testRestTemplate.postForEntity("/actuator/loggers/io.camunda.operate",request, String.class);
    assertThat(response.getStatusCodeValue()).isEqualTo(204);

    response = testRestTemplate.getForEntity("/actuator/loggers/io.camunda.operate",String.class);
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"configuredLevel\":\"TRACE\"");
  }

  @Override
  public TestRestTemplate getTestRestTemplate() {
    return testRestTemplate;
  }
}
