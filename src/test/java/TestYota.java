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

public class TestYota {
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    public static void setUp() {

            RestAssured.baseURI = System.getProperty("environment");
            //RestAssured.baseURI = prop.getProperty("restassured.baseURI");

            System.out.println("RestAssured Base URI: " + RestAssured.baseURI);

    }
    @Test
    public void businessStoryByAdmin() {
        String token = Steps.loggingInAccount("admin", "password");
        List<Long> numbers = Steps.gettingEmptyPhone(token);
        CustomerInfo customerInfo = Steps.postingCustomer(token, numbers);
        Steps.gettingCustomerById(token, customerInfo.getIdCustomer());
        Steps.findingCustomerByNumber(token, customerInfo.getPhone());
        Steps.changingCustomerStatus(token, customerInfo.getIdCustomer(), "admin");
    }
    @Test
    public void businessStoryByUser(){
        String token = Steps.loggingInAccount("user", "password");
        List<Long> numbers = Steps.gettingEmptyPhone(token);
        CustomerInfo customerInfo = Steps.postingCustomer(token,numbers);
        Steps.gettingCustomerById(token,customerInfo.getIdCustomer());
        Steps.findingCustomerByNumber(token,customerInfo.getPhone());
        Steps.changingCustomerStatus(token,customerInfo.getIdCustomer(),"user");

        }
    @Disabled
    @Test
    public void xmlRequest(){
        Long phone = Long.valueOf("79280340916");
        String token = Steps.loggingInAccount("admin","password");
        Steps.findingCustomerByNumber(token,phone);
    }
}