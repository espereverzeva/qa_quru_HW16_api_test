package tests;

import models.registration.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegistrationTests {

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

        RegistrationBodyModel data = new RegistrationBodyModel(username, password);

        SuccessfulRegistrationResponseModel registrationResponse = given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .as(SuccessfulRegistrationResponseModel.class);

        assertEquals(username, registrationResponse.username());
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

        RegistrationBodyModel data = new RegistrationBodyModel(username, password);

        SuccessfulRegistrationResponseModel registrationResponse_1 = given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .as(SuccessfulRegistrationResponseModel.class);

        assertEquals(username, registrationResponse_1.username());

        ExistingUserResponseModel registrationResponse_2 = given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(400)
                .extract()
                .as(ExistingUserResponseModel.class);

        String expectedError = "A user with that username already exists.";
        assertEquals(expectedError, registrationResponse_2.username().get(0));
    }

    @Test
    @DisplayName("В заголовках отсутствует contentType")
    public void unsupportedMediaType415Tests() {

        RegistrationBodyModel data = new RegistrationBodyModel(username, password);

        UnsupportedMediaTypeResponseModel registrationResponse = given()
                .log().all()
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(415)
                .extract()
                .as(UnsupportedMediaTypeResponseModel.class);

        String expectedError = "Unsupported media type \"text/plain; charset=ISO-8859-1\" in request.";
        assertEquals(expectedError, registrationResponse.detail());
    }

    @Test
    @DisplayName("\"Username\" состоит из нескольких слов")
    public void invalidUsername400Tests() {
        Faker faker = new Faker();
        username = faker.name().fullName();
        password = faker.name().firstName();

        RegistrationBodyModel data = new RegistrationBodyModel(username, password);

        InvalidUserNameResponseModel registrationResponse = given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(400)
                .extract()
                .as(InvalidUserNameResponseModel.class);

        String expectedError = "Enter a valid username. This value may contain only letters, numbers, and @/./+/-/_ characters.";
        assertEquals(expectedError, registrationResponse.username().get(0));
    }

    @Test
    @DisplayName("Регистрация с пустым полем \"Username\"")
    public void emptyUsernameFieldRegistration400Test() {

        RegistrationBodyModel data = new RegistrationBodyModel("", password);

        FieldRequiredResponseModel registrationResponse = given()
                .log().all()
                .contentType(JSON)
                .body(data)
                .when()
                .post("http://bookclub.qa.guru:8000/api/v1/users/register/")
                .then()
                .log().all()
                .statusCode(400)
                .extract()
                .as(FieldRequiredResponseModel.class);

        String expectedError = "This field may not be blank.";
        assertEquals(expectedError, registrationResponse.username().get(0));
    }
}

