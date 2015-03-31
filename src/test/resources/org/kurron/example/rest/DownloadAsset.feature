Feature: Download An Asset
  In order to use assets stored on the network
  REST API users should be able to download their previously stored assets

  Background:
    Given an asset has previously been uploaded
    And each request contains an X-Correlation-Id header filled in with a unique value
    And an Accept header filled in with the desired media-type of the bits to be downloaded

  @happy
  Scenario: Successful Download
    When a GET request is made to the URI
    Then a response with a 200 HTTP status code is returned
    And the Content-Type header matches the Accept header
    And the body contains the asset

  @sad
  Scenario: Unknown URI
    Given a URI that does not match anything in the system
    When a GET request is made to the URI
    Then a response with a 404 HTTP status code is returned
    And the hypermedia control describing the unknown asset is returned

  @sad @slow
  Scenario: Expired URI
    Given a URI of an expired asset
    When a GET request is made to the URI
    Then a response with a 404 HTTP status code is returned
    And the hypermedia control describing the unknown asset is returned
