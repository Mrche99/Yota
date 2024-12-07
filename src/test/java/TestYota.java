import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestYota {
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    public static void setUp() {
        Properties prop = new Properties();
        try (InputStream input = RestAssuredConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Файл свойств application.properties не найден в test ресурсах!");
            }

            // Загружаем свойства
            prop.load(input);

            // Устанавливаем baseURI для RestAssured
            RestAssured.baseURI = prop.getProperty("restassured.baseURI");

            System.out.println("RestAssured Base URI: " + RestAssured.baseURI);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось загрузить файл свойств", e);
        }
    }
    @ParameterizedTest
    @CsvSource({
            "admin,password",
            "user,password"
    })
    public void authorization(String userName, String password){
        Steps.loggingInAccount(userName,password);
    }

    @ParameterizedTest
    @CsvSource({
            "admin,password",
            "user,password"
    })
    public void businessStory(String userName, String password){
        String token = Steps.loggingInAccount(userName, password);
        List<Long> numbers = Steps.gettingEmptyPhone(token);
        CustomerInfo customerInfo = Steps.postingCustomer(token,numbers);
        System.out.println(customerInfo.getIdCustomer());
        Steps.gettingCustomerById(token,customerInfo.getIdCustomer());
        Steps.findingCustomerByNumber(token,customerInfo.getPhone());
        Steps.changingCustomerStatus(token,customerInfo.getIdCustomer(),userName);
    }
    @Disabled
    @Test
    public void xmlRequest(){
        Long phone = Long.valueOf("79280340916");
        String token = Steps.loggingInAccount("admin","password");
        Steps.findingCustomerByNumber(token,phone);
    }
}