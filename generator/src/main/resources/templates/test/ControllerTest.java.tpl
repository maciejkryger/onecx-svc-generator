package {{package}}.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import {{package}}.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import {{generatedModelPackage}}.{{generatedDto}};
import {{generatedModelPackage}}.{{generatedInternalSearchCriteria}};
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@GenerateKeycloakClient(
        clientName = "{{entityField}}InternalTestClient",
        scopes = { "{{scopePrefix}}:read", "{{scopePrefix}}:write", "{{scopePrefix}}:delete" }
)
class {{entity}}ControllerTest extends AbstractTest {

    String token;
    String idToken;

    @BeforeEach
    void setup() {
        token = keycloakClient.getClientAccessToken("{{entityField}}InternalTestClient");
        idToken = createToken("org1");
    }

    @Test
    void create{{entity}}Test() {
        {{testCreateDtoBody}}

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post("/internal/{{resourcePath}}")
                .then()
                .statusCode(201);
    }

    @Test
    void get{{entity}}ByIdTest() {
        String id = create{{entity}}AndReturnId();

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .when()
                .get("/internal/{{resourcePath}}/{id}", id)
                .then()
                .statusCode(200);
    }

    @Test
    void update{{entity}}Test() {
        String id = create{{entity}}AndReturnId();

        {{testUpdateDtoBody}}

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .put("/internal/{{resourcePath}}/{id}", id)
                .then()
                .statusCode(200);
    }

    @Test
    void delete{{entity}}Test() {
        String id = create{{entity}}AndReturnId();

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .when()
                .delete("/internal/{{resourcePath}}/{id}", id)
                .then()
                .statusCode(204);
    }

    @Test
    void search{{resourceOperationPlural}}Test() {
        {{testSearchCriteriaBody}}

        given()
                .auth().oauth2(token)
                .header(APM_HEADER_PARAM, idToken)
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post("/internal/{{resourcePath}}/search")
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