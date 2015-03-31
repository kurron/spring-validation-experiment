Feature: Upload An Asset
  In order to temporarily store assets on the network
  REST API users should be able to upload arbitrarily large assets

  Background:
    Given each request contains an X-Correlation-Id header filled in with a unique value
    And a Content-Type header filled in with media-type of the uploaded asset
    And an X-Expiration-Minutes header filled in with the number of minutes the asset should be available
    And a Content-Length header filled in with the size, in bytes, of the asset being uploaded
    And an Accept header filled in with the desired media-type of the returned hypermedia control

  @happy
  Scenario: Successful Upload
    Given an asset to be uploaded
    When a POST request is made with the asset in the body
    Then a response with a 201 HTTP status code is returned
    And the Location header contains the URI of the uploaded asset
    And the hypermedia control contains the URI of the uploaded asset
    And the hypermedia control contains the meta-data of the uploaded asset

  @sad
  Scenario: Failed Upload
    Given an asset that is too large
    When a POST request is made with the asset in the body
    Then a response with a 413 HTTP status code is returned
    And the hypermedia control describing the size problem is returned

