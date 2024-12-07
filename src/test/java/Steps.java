import PojoBody.ChangingStatusBody;
import PojoBody.LoginRequest;
import PojoBody.PostingCustomerBody;
import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;

public class Steps {
    public static String loggingInAccount(String username, String password) {
        //Форматируемая строка запроса, зависит от переедаемых логина и пароля в метод
        LoginRequest loginRequest = new LoginRequest(username, password);
        Gson gson = new Gson();
        String requestBody = gson.toJson(loginRequest);
        System.out.println(requestBody);

        Response response = given()//Запрос к сервису, с передачей логина и пароля для авторизации
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract().response();
        String token = response.jsonPath().getString("token");
        System.out.println(token);
        return token;
    }
    public static List<Long> gettingEmptyPhone(String token){
        AtomicReference<List<Long>> result = new AtomicReference<>();
        Awaitility.await()
                .atMost(1,TimeUnit.MINUTES)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Response response = RestAssured.given()
                            .header("authToken", token)
                            .get("simcards/getEmptyPhone")
                            .then()
                            .extract().response();
                    if (response.getStatusCode() == 200){
                        List<Long> phones = response.jsonPath().getList("phones.phone", Long.class);
                        if (!phones.isEmpty()){
                            result.set(phones);
                            return true;
                        }
                    } else {String errorMessage = response.jsonPath().getString("errorMessage");
                        System.out.println(errorMessage);
                        return false;
                    }
                    return false;
                });
        System.out.println(result);
        return result.get();


    }

    public static CustomerInfo postingCustomer(String token, List<Long> phones){

        AtomicReference<String> temp = new AtomicReference<>();
        AtomicReference<Long> successfulPhone = new AtomicReference<>();
        Awaitility.await().atMost(1,TimeUnit.MINUTES)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    for(Long phone:phones){
                        Map<String,String> additionalParameters = Map.of("string", "test");
                        PostingCustomerBody postingCustomerBody = new PostingCustomerBody("Test",String.valueOf(phone),additionalParameters);
                        Gson gson = new Gson();
                        String requestBody = gson.toJson(postingCustomerBody);
                        Response response = RestAssured.given()
                                . given() //Запрос для отправки нового пользователя в сервис
                                .header("authToken", token)
                                .contentType(ContentType.JSON)
                                .body(requestBody)
                                .when()
                                .post("/customer/postCustomer")
                                .then().extract().response();
                    if(response.statusCode() == 200) {
                        temp.set(response.jsonPath().getString("id"));
                        successfulPhone.set(phone);
                    return true;
                    }
                }
                    return false;
                });
        System.out.println(temp);
        System.out.println(successfulPhone);
        CustomerInfo customerInfo = new CustomerInfo(temp.get(),successfulPhone.get());
        return customerInfo;
    }




    public static void gettingCustomerById(String token,String idCustomer){
        String pathToEndPoint = String.format("customer/getCustomerById?customerId=%s",idCustomer);
             Awaitility.await()
                    .atMost(130,TimeUnit.SECONDS)
                     .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> {
                    Response response = RestAssured.given()
                            .header("authToken", token)
                            .get(pathToEndPoint)
                            .then()
                            .extract().response();
                        if ("ACTIVE".equals(response.jsonPath().getString("return.status"))) {//Если номер активировался, выводим паспортные данные, доп. параметры
                            System.out.println(response.jsonPath().getString("return.status"));
                            System.out.println(response.jsonPath().getString("return.additionalParameters"));
                            System.out.println(response.jsonPath().getString("return.pd"));
                            return true;
                        }
                    return false;
                    });
    }

    public static void findingCustomerByNumber(String token,Long phoneNumber){
        //Дополняем xml запрос переменными, полученными ранее
        String xmlTemplate = null;
        try {
            // Чтение файла из ресурсов
            xmlTemplate = new String(Files.readAllBytes(Paths.get("src/test/resources/xmlTemplate.xml")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read XML template", e);
        }

        // Замена плейсхолдеров на реальные значения
        String xmlRequestBody = xmlTemplate
                .replace("{authToken}", token)
                .replace("{phoneNumber}", String.valueOf(phoneNumber));

        Response response = given()
                .header("Content-Type","application/xml")
                .body(xmlRequestBody)
                .when()
                .post("/customer/findByPhoneNumber").then().statusCode(200).extract().response();
        String idCustomer = response.xmlPath().getString("Envelope.Body.customerId");
        if (idCustomer != null) {
            System.out.println(idCustomer);
        }else{
            System.out.println("Данный номер вам не принадлежит");
        }
    }

    public static void changingCustomerStatus(String token,String idCustomer,String role){

        int httpStatusCode = (role.equals("admin")) ? 200: 401;
        if(token != null){
            ChangingStatusBody changingStatusBody = new ChangingStatusBody("New");
            Gson gson = new Gson();
            String bodyMessage = gson.toJson(changingStatusBody);
            String customerStatus = "NEW";
            String statusCustomerParam = String.format("/customer/getCustomerById?customerId=%s", idCustomer);//передаем в параметры id полученный ранее

            String pathToEndpoint = String.format("/customer/%s/changeCustomerStatus", idCustomer);
            RestAssured.given()//Проверяем возможность изменения статуса с помощью двух ролей
                    .header("authToken", token)
                    .contentType(ContentType.JSON)
                    .body(bodyMessage)
                    .when()
                    .post(pathToEndpoint).then().statusCode(httpStatusCode);
            //Используем ещё один запрос для проверки изменения статуса
            Response response = given()
                    .header("authToken", token)
                    .when()
                    .get(statusCustomerParam)
                    .then()
                    .statusCode(200)
                    .extract().response();
            String statusGet = response.jsonPath().getString("return.status");
            System.out.println(statusGet);
        }
    }
}

