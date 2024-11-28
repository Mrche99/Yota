import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;



import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestYota {
    private static String tokenValueAdmin;
    private static String tokenValueUser;
    private static Long firstNumberAdm;
    private static Long firstNumberUser;
    private static String idCustomerAdm;
    private static String idCustomerUser;
    static boolean newCustomerAdm = false;
    static boolean newCustomerUser = false;
    static String additionalParameters ="test parameters";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost:8090";
    }



    @ParameterizedTest //Параметризованный тест, одновременно прогоняет тест от лица администратора и пользователя
    @CsvSource({
        "admin,password",
            "user,password"
    })
    @Order(1)
    public void loggingInAccount(String username, String password) {
        //Форматируемая строка запроса, зависит от переедаемых логина и пароля в метод
        String requestBody = String.format(""" 
                {
                    "login":"%s",
                    "password":"%s"
                }
                """,username,password);

        Response response = given()//Запрос к сервису, с передачей логина и пароля для авторизации
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract().response();
        //Отдельно запоминаем токены для админа и пользователя, чтобы в дальнейшем использовать для выполнения параллельных тестов
        if (username.equals("admin")){
            tokenValueAdmin = response.jsonPath().getString("token");
        }
        else {
            tokenValueUser = response.jsonPath().getString("token");
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"admin","user"})
    @Order(2)
    public void gettingEmptyPhone(String role){
        String token = (role.equals("admin")) ? tokenValueAdmin : tokenValueUser;
        boolean flag = true;
        int attempts = 1;
        int maxAttempts = 5;
        String firstNumberStr = "";
        String responseBody = "";
        //Так как сервер может выдавать ошибку пробуем обращаться несколько раз для получения номера с помощью цикла
        while (flag && attempts <= maxAttempts) {
            Response response =
                    given()
                        .header("authToken", token)
                    .when()
                        .get("/simcards/getEmptyPhone")
                        .then()
                        .extract().response();
            if (response.statusCode() == 200) {
                responseBody = response.jsonPath().getString("phones.phone");
                flag = false;
            } else if (response.statusCode() == 500) {
                System.out.println("Ошибка сервера 500. Попытка " + (attempts));//Если выдает ошибку, делаем паузу
                try {
                    Thread.sleep(2000);  // Задержка 2 секунды между попытками
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Получен неожиданный статус код: " + response.statusCode());
                break;  // Выход из цикла при получении другого неудачного кода
            }
            attempts++;
        }

            int commaIndex = responseBody.indexOf(",");

            // Извлекаем подстроку от начала до запятой
            if (commaIndex != -1) {
                // Есь несколько чисел, берём первое до запятой
                try {

                    firstNumberStr = responseBody.substring(1, commaIndex).trim();
                } catch (Exception e) {
                    System.out.println("Ошибка приведения");
                }
            } else {
                // Если только одно число в ответе
                firstNumberStr = responseBody.replace("[", " ").replace("]", " ").trim();
            }
            if(firstNumberStr.isEmpty()){
                System.out.println("Не удалось получить номер");
            }
            else {
                if (role.equals("admin")) {
                    firstNumberAdm = Long.valueOf(firstNumberStr);
                }
                else{
                    firstNumberUser = Long.valueOf(firstNumberStr);
                }

                System.out.println(Long.valueOf(firstNumberStr));
            }

    }
    @ParameterizedTest
    @ValueSource(strings = {"admin","user"})
    @Order(3)
    public void postingCustomer(String role){
        String token = (role.equals("admin")) ? tokenValueAdmin : tokenValueUser;//получаем токен в зависимости от роли
        Long firstNumber = (role.equals("admin")) ? firstNumberAdm : firstNumberUser;//получаем число в зависимости от роли
        String name = "testName1";
        String requestBody =String.format
                ("""
                {
                    "name":"%s",
                    "phone":"%s",
                    "additionalParameters":{
                    "string":"%s"
                    }
                    }""",name,firstNumber,additionalParameters) ;//Тело сообщения
        Response response =
                given() //Запрос для отправки нового пользователя в сервис
                    .header("authToken", token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                .when()
                    .post("/customer/postCustomer")
                    .then().extract().response();
        String error = response.jsonPath().getString("errorMessage");

        if (error==null && role.equals("admin")) {//В зависимости от роли выводим данные, и сохраняем переменную id для дальнейшего использования
            idCustomerAdm = response.jsonPath().getString("id");
            System.out.println(response.jsonPath().getString("id"));
            newCustomerAdm = true;
        }
        else if (error==null && role.equals("user")) {//В зависимости от роли выводим данные, и сохраняем переменную id для дальнейшего использования
            idCustomerUser = response.jsonPath().getString("id");
            System.out.println(response.jsonPath().getString("id"));
            newCustomerUser = true;
        }
        else {
            System.out.println(error);
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"admin","user"})
    @Order(4)
    public void gettingCustomerById(String role){
        //Получаем переменные в зависимости от роли
        String token = (role.equals("admin")) ? tokenValueAdmin : tokenValueUser;
        boolean newCustomer = (role.equals("admin")) ? newCustomerAdm : newCustomerUser;
        String idCustomer = (role.equals("admin")) ? idCustomerAdm : idCustomerUser;
        if (newCustomer) {
            boolean isActiveted = false;
            int maxTimeForActivation = 130;
            int interval = 10;
            String passportData = "";
            String statusActiveted;
            String statusCustomerParam = String.format("/customer/getCustomerById?customerId=%s", idCustomer);
            //Ждем пока не активируется номер, повторяя запрос каждые 10 сек
            for (int elapsedTime = 0; elapsedTime < maxTimeForActivation; elapsedTime += interval) {

                Response response = given()
                        .header("authToken", token)
                        .when()
                        .get(statusCustomerParam)
                        .then()
                        .statusCode(200)
                        .extract().response();
                statusActiveted = response.jsonPath().getString("return.status");
                if ("ACTIVE".equalsIgnoreCase(statusActiveted)) {//Если номер активировался, выводим паспортные данные, доп. параметры
                    passportData = response.jsonPath().getString("return.pd");
                    System.out.println(response.jsonPath().getString("return.status"));
                    System.out.println(response.jsonPath().getString("return.additionalParameters"));
                    System.out.println(passportData);
                    isActiveted = true;
                    break;
                }
                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Тест был прерван", e);
                }

            }
        }
        else {
            System.out.println("Невозможно выполнить тест, без указания Id");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin","user"})
    @Order(5)
    public void findingCustomerByNumber(String role){
        String token = (role.equals("admin")) ? tokenValueAdmin : tokenValueUser;
        Long firstNumber = (role.equals("admin")) ? firstNumberAdm : firstNumberUser;
        //Дополняем xml запрос переменными, полученными ранее
        String xmlTemplate = String.format("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ns3:Envelope xmlns:ns2="soap" xmlns:ns3="http://schemas.xmlsoap.org/soap/envelope">
                    <ns2:Header>
                        <authToken>%s</authToken>
                    </ns2:Header>
                    <ns2:Body>
                        <phoneNumber>%d</phoneNumber>
                    </ns2:Body>
                </ns3:Envelope>
                """,token,firstNumber);
        Response response = given()
                .header("Content-Type","application/xml")
                .body(xmlTemplate)
                .when()
                .post("/customer/findByPhoneNumber").then().statusCode(200).extract().response();
        String idCustomer = response.xmlPath().getString("Envelope.Body.customerId");
        if (idCustomer != null) {
            System.out.println(idCustomer);
        }else{
            System.out.println("Данный номер вам не принадлежит");
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"admin","user"})
    @Order(6)
    public void changingCustomerStatus(String role){
        String token = (role.equals("admin")) ? tokenValueAdmin : tokenValueUser;
        String idCustomer = (role.equals("admin")) ? idCustomerAdm : idCustomerUser;
        int httpStatusCode = (role.equals("admin")) ? 200: 401;
            String customerStatus = "NEW";
            String statusCustomerParam = String.format("/customer/getCustomerById?customerId=%s", idCustomer);//передаем в параметры id полученный ранее
            String bodyMessage = String.format("""
                {
                "status":"%s"
                }
                """,customerStatus);
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