package arvato.assignment;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This class contains the step definitions for verifying bank account functionality.
 */
public class ValidateBankAccountStepDefinitions {
    //    Custom logging filters for recording request and response data on extent report
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private PrintStream printStream = new PrintStream(outputStream, true);
    RequestLoggingFilter customRequestLoggingFilter = new RequestLoggingFilter(printStream);
    ResponseLoggingFilter customResponseLoggingFilter = new ResponseLoggingFilter(printStream);

    private Response response;
    private RequestSpecification request = given().
            header("Content-Type", "application/json").
            and().header("Cache-Control", "no-cache").
            and().filters(customRequestLoggingFilter, customResponseLoggingFilter);

    /**
     * This hook is used here to write request and response data to extent reports
     *
     * @param scenario an instance of Cucumber scenario class
     */
    @After
    public void afterScenario(Scenario scenario) {
        scenario.write(outputStream.toString());
    }

    /**
     * Adds appropriate header to request based on value of validity parameter
     *
     * @param validity string value from valid/invalid/no
     */
    @Given("a request with {string} JWT token")
    public void aRequestWithJWTToken(String validity) {
        switch (validity) {
            case "valid":
                request = request.with().header("X-Auth-Key", "Q7DaxRnFls6IpwSW1SQ2FaTFOf7UdReAFNoKY68L");
                break;
            case "invalid":
                request = request.with().header("X-Auth-Key", "Q7DaxRnFls6IpwSW");
                break;
            case "no":
//                Do not update header.
                break;
        }
    }

    /**
     * Adds bank account details to request body.
     *
     * @param accountType takes a value valid IBAN/invalid IBAN
     * @param country     takes value supported/unsupported
     */
    @And("{string} account from {string} country")
    public void aAccountFromCountry(String accountType, String country) {
        if (accountType.equals("valid IBAN")) {
            if (country.equals("supported")) {
//                valid IBAN account from supported country
                request = request.body("{\"bankAccount\": \"DE91100000000123456789\"}");
            } else {
//                valid IBAN account from unsupported country
                request = request.body("{\"bankAccount\": \"GB09HAOE91311808002317\"}");
            }
        } else {
//            invalid IBAN; valid IBAN CH5604835012345678009 modified to to fail sanity check
            request = request.body("{\"bankAccount\": \"CH5604835002345678009\"}");
        }
    }

    /**
     * Sends the request to server using POST method
     */
    @When("request is posted to api")
    public void requestIsPostedToAPI() {
        String URI = "https://dev.horizonafs.com/ecommercerest/api/v3/validate/bank-account";
        request.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        response = request.post(URI);
    }

    /**
     * Validates the status code from response object and code if it is present in riskCheckMessage or in response body.
     *
     * @param statusCode integer value of status code
     * @param errorCode  string value of error code
     */
    @Then("api returns {int} status code and {string} error code")
    public void apiReturnsStatusCodeAndErrorCode(int statusCode, String errorCode) {
        response.then().statusCode(statusCode);
        if (!errorCode.equals("no")) {
            if (statusCode == 200) {
//                if error code is 200, code will be present in riskCheckMessages. This provides implicit test for non empty riskCheckMessages. An explicit validation can be done using riskCheckMessagesIsNonEmpty method.
                response.then().body("riskCheckMessages[\"code\"][0]", equalTo(errorCode));
            } else {
//                if error code is not 200 code will be present in response body
                response.then().body("code[0]", equalTo(errorCode));
            }
        }
    }

    /**
     * Validates if the isValid is correct
     *
     * @param isValid takes a value from valid/in-valid
     */
    @And("bank account is {string}")
    public void bankAccountIs(String isValid) {
        boolean validity = isValid.toLowerCase().equals("valid");
        response.then().body("isValid", equalTo(validity));
    }

    /**
     * Validates if body contains message for denied authorization.
     */
    @And("body contains invalid authorization message")
    public void bodyContainsInvalidAuthorizationMessage() {
        response.then().body("message", equalTo("Authorization has been denied for this request."));
    }

    /**
     * Validates if riskCheckMessages is non-empty.
     */
    @And("riskCheckMessages is non-empty")
    public void riskCheckMessagesIsNonEmpty() {
        response.then().body("riskCheckMessages", notNullValue());
    }

    /**
     * Generates numeric account numbers of given length and attaches it to request body
     *
     * @param length of account number
     */
    @And("non-IBAN account of given length {int}")
    public void nonIBANAccountOfGivenLength(int length) {
        String account = randomNumeric(length);
        request = request.body("{\"bankAccount\": " + account + "}");
    }

    /**
     * Leaves the request body empty.
     */
    @And("request has empty body")
    public void requestHasEmptyBody() {
    }
}
