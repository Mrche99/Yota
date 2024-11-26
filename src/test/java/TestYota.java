import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class TestYota {

    @Test
    public void loggingInAccount(){
        RestAssured.baseURI = "http://localhost:8090";
        String requestBody = """
                {
                    "login":"admin",
                    "password":"password"
                }
                """;

        Response response =RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract().response();
                //.body("token",equalTo("348585fc58ca499b8e61a4ed836a5f6e"));
        JsonPath jsonPath = response.jsonPath();
        String tokenValue = jsonPath.getString("token");

    }

}
