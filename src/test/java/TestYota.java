import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import java.util.List;


public class TestYota {
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost:8090";
    /*    adminToken = Steps.loggingInAccount("admin","password");
        userToken = Steps.loggingInAccount("user","password");*/
    }
    @Test
    public void authorizationAdmin(){
        Steps.loggingInAccount("admin","password");
    }

    @Test
    public void businessStoryAdmin(){
        String token = Steps.loggingInAccount("admin", "password");
        List<Long> numbers = Steps.gettingEmptyPhone(token);
        CustomerInfo customerInfo = Steps.postingCustomer("test",token,numbers);
        System.out.println(customerInfo.getIdCustomer());
        Steps.gettingCustomerById(token,customerInfo.getIdCustomer());
        Steps.findingCustomerByNumber(token,customerInfo.getPhone());
        Steps.changingCustomerStatus(token,customerInfo.getIdCustomer(),"admin");
    }

    @Test
    public void businessStoryUser(){
        String token = Steps.loggingInAccount("user", "password");
        List<Long> numbers = Steps.gettingEmptyPhone(token);
        CustomerInfo customerInfo = Steps.postingCustomer("test",token,numbers);
        System.out.println(customerInfo.getIdCustomer());
        Steps.gettingCustomerById(token,customerInfo.getIdCustomer());
        Steps.findingCustomerByNumber(token,customerInfo.getPhone());
        Steps.changingCustomerStatus(token,customerInfo.getIdCustomer(),"user");
    }

    @Test
    public void xmlRequest(){
        Long phone = Long.valueOf("79280340916");
        String token = Steps.loggingInAccount("admin","password");
        Steps.findingCustomerByNumber(token,phone);
    }
}