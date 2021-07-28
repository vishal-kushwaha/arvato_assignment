Feature: Bank account Validation
  Validates and evaluates the account in the context of direct debit payment. Allows to submit IBAN and check if it’s valid. Method has 2-step validation:\n
  - length based validation, bank account considered to be valid if its length is between 7 and 34 chars, 400.00x error returned if validation fails.\n
  - external validation. 200(OK) with non empty riskCheckMessages returned if validation fails, otherwise isValid = true returned.\n
  - Validation works only for accounts written in IBAN format. Validation supports IBANs from the countries Austria, Germany, Switzerland, Finland, Sweden, Norway, and Denmark. Response message with error code 200.909 is returned If country is not supported.

  Scenario: request with valid IBAN account of supported country
    Given a request with "valid" JWT token
    And "valid IBAN" account from "supported" country
    When request is posted to api
    Then api returns 200 status code and "no" error code
    And bank account is "valid"

  Scenario: request with invalid JWT token
    Given a request with "invalid" JWT token
    And  "valid IBAN" account from "supported" country
    When request is posted to api
    Then api returns 401 status code and "no" error code
    And body contains invalid authorization message

  Scenario: request without JWT token
    Given a request with "no" JWT token
    And  "valid IBAN" account from "supported" country
    When request is posted to api
    Then api returns 401 status code and "no" error code
    And body contains invalid authorization message

  Scenario Outline: request with non-IBAN account of different lengths
    Given a request with "valid" JWT token
    And non-IBAN account of given length <length>
    When request is posted to api
    Then api returns <statusCode> status code and "<errorCode>" error code

    Examples:
      | length | statusCode | errorCode |
#    <invalid partition boundary>
      | 6      | 400        | 400.006   |
#    </invalid partition boundary>
#    <valid partition boundaries>
      | 7      | 200        | 200.908   |
      | 8      | 200        | 200.908   |
      | 33     | 200        | 200.908   |
      | 34     | 200        | 200.908   |
#    </valid partition boundaries>
#    <invalid partition>
      | 35     | 400        | 400.005   |
#    </invalid partition>

  Scenario: request with valid IBAN account of unsupported country
    Given a request with "valid" JWT token
    And "valid IBAN" account from "unsupported" country
    When request is posted to api
    Then api returns 200 status code and "200.909" error code
    And bank account is "invalid"
    And riskCheckMessages is non-empty

  Scenario: request with invalid IBAN account
    Given a request with "valid" JWT token
    And "invalid IBAN" account from "any" country
    When request is posted to api
    Then api returns 200 status code and "200.908" error code
    And riskCheckMessages is non-empty
    And bank account is "invalid"

  Scenario: request with empty body
    Given a request with "valid" JWT token
    And request has empty body
    When request is posted to api
    Then api returns 400 status code and "400.001" error code
    And riskCheckMessages is non-empty