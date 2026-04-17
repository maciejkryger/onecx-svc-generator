package {{package}}.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import {{package}}.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import {{generatedModelPackage}}.{{generatedDto}};
import {{generatedExternalModelPackage}}.{{generatedExternalSearchCriteria}};
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GenerateKeycloakClient(
        clientName = "{{entityField}}ExternalTestClient",
        scopes = { "{{scopePrefix}}:read", "{{scopePrefix}}:write" }
)
class {{entity}}ControllerTest extends AbstractTest {

    String token;
    String idToken;

    @BeforeEach
    void setup() {
        token = keycloakClient.getClientAccessToken("{{entityField}}ExternalTestClient");
        idToken = createToken("org1");
    }

    @Test
    void get{{entity}}ByIdTest() {
        String id = create{{entity}}AndReturnId();

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .when()
                .get("/v1/{{resourcePath}}/{id}", id)
                .then()
                .statusCode(200);
    }

    @Test
    void search{{resourceOperationPlural}}Test() {
        create{{entity}}AndReturnId();

        {{testExternalSearchCriteriaBody}}

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/{{resourcePath}}/search")
                .then()
                .statusCode(200);
    }

    private String create{{entity}}AndReturnId() {
        {{testCreateDtoBody}}

        return given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post("/internal/{{resourcePath}}")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}