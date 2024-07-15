/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.uknwauthcheckerapi.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.test.Helpers._
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.errors._
import uk.gov.hmrc.uknwauthcheckerapi.generators.ValidAuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, AuthorisationResponse, AuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.services.{IntegrationFrameworkService, ValidationService}
import uk.gov.hmrc.uknwauthcheckerapi.utils.JsonErrors

import java.time.LocalDate
import scala.concurrent.Future

class AuthorisationsControllerSpec extends BaseSpec {

  private val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]
  private val mockValidationService:           ValidationService           = mock[ValidationService]
  private val controller = new AuthorisationsController(stubComponents, mockIntegrationFrameworkService, mockValidationService)

  "AuthorisationsController" should {
    "return OK (200) with authorised eoris when request has valid date and eoris" in {
      forAll { authorisationRequest: AuthorisationRequest =>
        val expectedResponse = AuthorisationsResponse(
          LocalDate.parse(authorisationRequest.date),
          authorisationRequest.eoris.map(r => AuthorisationResponse(r, authorised = true))
        )

        when(mockValidationService.validateRequest(any()))
          .thenReturn(
            Right(authorisationRequest)
          )

        when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
          .thenReturn(EitherT.rightT(expectedResponse))

        val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

        val result = controller.authorisations()(request)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponse)
      }
    }

    "return BAD_REQUEST (400) error when request json field is missing" in {
      val request = fakeRequestWithJsonBody(emptyJson)

      val jsError = JsError(
        Seq("date", "eoris").map { field =>
          (JsPath \ field, Seq(JsonValidationError(JsonErrors.pathMissing)))
        }
      )

      val expectedResponse = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      when(mockValidationService.validateRequest(any())).thenReturn(
        Left(ValidationDataRetrievalError(jsError))
      )

      val result = controller.authorisations()(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe expectedResponse
    }

    "return BAD_REQUEST (400) error when request json is malformed" in {
      val request = fakeRequestWithJsonBody(emptyJson)

      val jsError = JsError(
        Seq((JsPath \ "", Seq(JsonValidationError(JsonErrors.expectedJsObject))))
      )

      val expectedResponse = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      when(mockValidationService.validateRequest(any())).thenReturn(
        Left(ValidationDataRetrievalError(jsError))
      )

      val result = controller.authorisations()(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe expectedResponse
    }

    "return BAD_REQUEST (400) error when request error is custom" in {
      val request = fakeRequestWithJsonBody(emptyJson)

      val jsError = JsError(
        Seq((JsPath \ "", Seq(JsonValidationError("test error"))))
      )

      val expectedResponse = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      when(mockValidationService.validateRequest(any())).thenReturn(
        Left(ValidationDataRetrievalError(jsError))
      )

      val result = controller.authorisations()(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe expectedResponse
    }
  }

  "return BAD_REQUEST (400) error when getAuthorisations call has date error" in forAll { (validRequest: ValidAuthorisationRequest) =>
    val authorisationRequest: AuthorisationRequest = validRequest.request

    val error = BadRequestDataRetrievalError(invalidDateEisErrorMessage)

    val expectedResponse = Json.toJson(
      BadRequestApiError(invalidDateEisErrorMessage)
    )(ApiErrorResponse.badRequestApiErrorWrites)

    when(mockValidationService.validateRequest(any()))
      .thenReturn(
        Right(authorisationRequest)
      )

    when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
      .thenReturn(EitherT.leftT(error))

    val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

    val result = controller.authorisations()(request)

    status(result)        shouldBe BAD_REQUEST
    contentAsJson(result) shouldBe expectedResponse
  }

  "return BAD_REQUEST (400) error when getAuthorisations call has eori errors" in forAll { (validRequest: ValidAuthorisationRequest) =>
    val authorisationRequest: AuthorisationRequest = validRequest.request

    val error = BadRequestDataRetrievalError(invalidEorisEisErrorMessage)

    val expectedResponse = Json.toJson(
      BadRequestApiError(invalidEorisEisErrorMessage)
    )(ApiErrorResponse.badRequestApiErrorWrites)

    when(mockValidationService.validateRequest(any()))
      .thenReturn(
        Right(authorisationRequest)
      )

    when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
      .thenReturn(EitherT.leftT(error))

    val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

    val result = controller.authorisations()(request)

    status(result)        shouldBe BAD_REQUEST
    contentAsJson(result) shouldBe expectedResponse
  }

  "return BAD_REQUEST (400) error when getAuthorisations call has eori and dateerrors" in forAll { (validRequest: ValidAuthorisationRequest) =>
    val authorisationRequest: AuthorisationRequest = validRequest.request

    val error = BadRequestDataRetrievalError(invalidMixedEisErrorMessage)

    val expectedResponse = Json.toJson(
      BadRequestApiError(invalidMixedEisErrorMessage)
    )(ApiErrorResponse.badRequestApiErrorWrites)

    when(mockValidationService.validateRequest(any()))
      .thenReturn(
        Right(authorisationRequest)
      )

    when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
      .thenReturn(EitherT.leftT(error))

    val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

    val result = controller.authorisations()(request)

    status(result)        shouldBe BAD_REQUEST
    contentAsJson(result) shouldBe expectedResponse
  }

  "return SERVICE_UNAVAILABLE (503) when integration framework service returns BadGatewayDataRetrievalError" in {
    forAll { (authorisationRequest: AuthorisationRequest) =>
      val expectedResponse = Json.toJson(
        ServiceUnavailableApiError
      )(ApiErrorResponse.writes.writes)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(BadGatewayDataRetrievalError()))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe SERVICE_UNAVAILABLE
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return BAD_REQUEST (400) when integration framework service returns BadRequestDataRetrievalError with a date error" in {
    forAll { (authorisationRequest: AuthorisationRequest) =>
      val expectedResponse = Json.toJson(
        BadRequestApiError(invalidDateEisErrorMessage)
      )(ApiErrorResponse.badRequestApiErrorWrites)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(BadRequestDataRetrievalError(invalidDateEisErrorMessage)))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return FORBIDDEN (403) when integration framework service returns ForbiddenDataRetrievalError" in {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        ForbiddenApiError
      )(ApiErrorResponse.writes.writes)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(ForbiddenDataRetrievalError(errorMessage)))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return METHOD_NOT_ALLOWED (405) when integration framework service returns MethodNotAllowedDataRetrievalError" in {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        MethodNotAllowedApiError
      )(ApiErrorResponse.writes.writes)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(MethodNotAllowedDataRetrievalError(errorMessage)))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe METHOD_NOT_ALLOWED
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return INTERNAL_SERVER_ERROR (500) when integration framework service returns InternalServerDataRetrievalError" in {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        InternalServerApiError
      )(ApiErrorResponse.writes.writes)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(InternalServerDataRetrievalError(errorMessage)))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return INTERNAL_SERVER_ERROR (500) when integration framework service returns InternalUnexpectedDataRetrievalError" in {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        InternalServerApiError
      )(ApiErrorResponse.writes.writes)

      when(mockValidationService.validateRequest(any()))
        .thenReturn(
          Right(authorisationRequest)
        )

      when(mockIntegrationFrameworkService.getAuthorisations(any())(any()))
        .thenReturn(EitherT.leftT(InternalUnexpectedDataRetrievalError(errorMessage, new Exception())))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

      val result = controller.authorisations()(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return NOT_ACCEPTABLE (406) error when accept header is not present" in {
    forAll { authorisationRequest: AuthorisationRequest =>
      val headers = defaultHeaders.filterNot(_._1.equals(jsonAcceptHeader._1))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest), headers = headers)

      val result = controller.authorisations()(request)

      status(result)        shouldBe NOT_ACCEPTABLE
      contentAsJson(result) shouldBe contentAsJson(Future.successful(NotAcceptableApiError.toResult))
    }
  }

  "return NOT_ACCEPTABLE (406) error when content type header is not present" in {
    forAll { authorisationRequest: AuthorisationRequest =>
      val headers = defaultHeaders.filterNot(_._1.equals(jsonContentTypeHeader._1))

      val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest), headers = headers)

      val result = controller.authorisations()(request)

      status(result)        shouldBe NOT_ACCEPTABLE
      contentAsJson(result) shouldBe contentAsJson(Future.successful(NotAcceptableApiError.toResult))
    }
  }

}
