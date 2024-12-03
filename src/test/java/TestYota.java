import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import java.util.List;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestYota {
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost:8090";
    /*    adminToken = Steps.loggingInAccount("admin","password");
        userToken = Steps.loggingInAccount("user","password");*/
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

    @Test
    public void xmlRequest(){
        Long phone = Long.valueOf("79280340916");
        String token = Steps.loggingInAccount("admin","password");
        Steps.findingCustomerByNumber(token,phone);
    }
}