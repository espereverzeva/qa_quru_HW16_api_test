package tests;

import models.registration.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static specs.login.LoginSpec.wrongCredentialLoginResponseSpec;
import static specs.registation.RegistrationSpec.*;

public class RegistrationTests extends TestBase {

    String username;
    String password;

    @BeforeEach
    public void prepareTestData() {
        Faker faker = new Faker();
        username = faker.name().firstName();
        password = faker.name().firstName();
    }

    @Test
    @DisplayName("Регистрация с валидными данными")
    public void succesfullRegistrationTests() {

        RegistrationBodyModel registrationData = new RegistrationBodyModel(username, password);

        SuccessfulRegistrationResponseModel registrationResponse = given(registrationRequestSpec)
                .body(registrationData)
                .when()
                .post("/users/register/")
                .then()
                .spec(successfulRegistrationResponseSpec)
                .extract().
                as(SuccessfulRegistrationResponseModel.class);

        assertThat(registrationResponse.id()).isGreaterThan(0);
        assertThat(registrationResponse.username()).isEqualTo(username);
        assertThat(registrationResponse.firstName()).isEqualTo("");
        assertThat(registrationResponse.lastName()).isEqualTo("");
        assertThat(registrationResponse.email()).isEqualTo("");

        String ipAddrRegexp = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}"
                + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$";
        assertThat(registrationResponse.remoteAddr()).matches(ipAddrRegexp);

    }

    @Test
    @DisplayName("Регистрация с валидными данными_плохой пример")
    public void succesfullRegistrationTests_badPractics() {

        String data = "{\"username\": \"" + username + "\",\"password\": \"" + password + "\"}";

        given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(201)
                .body("username", is(username))
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("Регистрация существующего пользователя")
    public void existingUserTests() {

        RegistrationBodyModel registrationData = new RegistrationBodyModel(username, password);

        SuccessfulRegistrationResponseModel firstregistrationResponse = given(registrationRequestSpec)
                .body(registrationData)
                .when()
                .post("/users/register/")
                .then()
                .spec(successfulRegistrationResponseSpec)
                .extract()
                .as(SuccessfulRegistrationResponseModel.class);

        assertThat(firstregistrationResponse.username()).isEqualTo(username);

        ExistingUserResponseModel secondregistrationResponse = given(registrationRequestSpec)
                .body(registrationData)
                .when()
                .post("/users/register/")
                .then()
                .spec(existingUserRegistrationResponseSpec)
                .extract()
                .as(ExistingUserResponseModel.class);

        String expectedError = "A user with that username already exists.";
        String actualError = secondregistrationResponse.username().get(0);
        assertThat(actualError).isEqualTo(expectedError);
    }

    @Test
    @DisplayName("В заголовках отсутствует contentType")
    public void unsupportedMediaType415Tests() {

        RegistrationBodyModel registrationData = new RegistrationBodyModel(username, password);

        UnsupportedMediaTypeResponseModel registrationResponse = given()
                .log().all()
                .body(registrationData)
                .basePath("/api/v1")
                .when()
                .post("/users/register/")
                .then()
                .spec(unsupportedMediaTypeResponseSpec)
                .extract()
                .as(UnsupportedMediaTypeResponseModel.class);

        String expectedError = "Unsupported media type \"text/plain; charset=ISO-8859-1\" in request.";
        String actualError = registrationResponse.detail();
        assertThat(actualError).isEqualTo(expectedError);
    }

    @Test
    @DisplayName("\"Username\" состоит из нескольких слов")
    public void invalidUsername400Tests() {
        Faker faker = new Faker();
        username = faker.name().fullName();
        password = faker.name().firstName();

        RegistrationBodyModel registrationData = new RegistrationBodyModel(username, password);

        InvalidUserNameResponseModel registrationResponse = given(registrationRequestSpec)
                .body(registrationData)
                .when()
                .post("/users/register/")
                .then()
                .spec(invalidUserNameRegistrationResponseSpec)
                .extract()
                .as(InvalidUserNameResponseModel.class);

        String expectedError = "Enter a valid username. This value may contain only letters, numbers, and @/./+/-/_ characters.";
        String actualError = registrationResponse.username().get(0);
        assertThat(actualError).isEqualTo(expectedError);
    }

    @Test
    @DisplayName("Регистрация с пустым полем \"Username\"")
    public void emptyUsernameFieldRegistration400Test() {

        RegistrationBodyModel registrationData = new RegistrationBodyModel("", password);

        FieldRequiredResponseModel registrationResponse = given(registrationRequestSpec)
                .body(registrationData)
                .when()
                .post("/users/register/")
                .then()
                .spec(emptyUsernameFielRegistrationResponseSpec)
                .extract()
                .as(FieldRequiredResponseModel.class);

        String expectedError = "This field may not be blank.";
        String actualError = registrationResponse.username().get(0);
        assertThat(actualError).isEqualTo(expectedError);

    }
}

