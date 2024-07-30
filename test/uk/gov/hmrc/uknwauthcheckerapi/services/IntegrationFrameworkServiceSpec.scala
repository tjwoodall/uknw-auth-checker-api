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

package uk.gov.hmrc.uknwauthcheckerapi.services

import java.time.LocalDate
import scala.concurrent.Future

import com.google.inject.AbstractModule
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.http.Status._
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{BadGatewayException, UpstreamErrorResponse}
import uk.gov.hmrc.uknwauthcheckerapi.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.generators.ValidAuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models._
import uk.gov.hmrc.uknwauthcheckerapi.models.eis._
import uk.gov.hmrc.uknwauthcheckerapi.utils.JsonErrors

class IntegrationFrameworkServiceSpec extends BaseSpec {

  private lazy val service: IntegrationFrameworkService = injected[IntegrationFrameworkService]

  override def moduleOverrides: AbstractModule = new AbstractModule {
    override def configure(): Unit =
      bind(classOf[IntegrationFrameworkConnector]).toInstance(mockIntegrationFrameworkConnector)
  }

  "getEisAuthorisations" should {
    "return EisAuthorisationsResponse when call to the integration framework succeeds" in forAll {
      (validRequest: ValidAuthorisationRequest, date: LocalDate) =>
        val request = validRequest.request

        val expectedResponse = AuthorisationsResponse(
          date,
          request.eoris.map(r => AuthorisationResponse(r, authorised = true))
        )

        val expectedEisAuthorisationsResponse = EisAuthorisationsResponse(
          date,
          appConfig.authType,
          request.eoris.map(r => EisAuthorisationResponse(r, valid = true, 0))
        )

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.successful(expectedEisAuthorisationsResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Right(expectedResponse)
    }

    "return BadGatewayRetrievalError error when call to the integration framework fails with BAD_GATEWAY" in forAll {
      (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedResponse = new BadGatewayException(errorMessage)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(BadGatewayDataRetrievalError())
    }

    "return InternalUnexpectedDataRetrievalError error when call to the integration framework fails with a non fatal error" in forAll {
      (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedResponse = new Exception(errorMessage)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(InternalUnexpectedDataRetrievalError(expectedResponse.getMessage, expectedResponse))
    }

    "return BadRequestDataRetrievalError error when call to the integration framework fails with a BAD_REQUEST" in forAll {
      (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(
              errorCode = BAD_REQUEST,
              errorMessage = invalidEorisEisErrorMessage
            )
        )

        val expectedResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), BAD_REQUEST)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(BadRequestDataRetrievalError(invalidEorisEisErrorMessage))
    }

    "return ForbiddenDataRetrievalError error when call to the integration framework fails with a FORBIDDEN" in forAll {
      (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val expectedResponse = UpstreamErrorResponse("{}", FORBIDDEN)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(ForbiddenDataRetrievalError())
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with a INTERNAL_SERVER_ERROR" in forAll {
      (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(errorCode = INTERNAL_SERVER_ERROR)
        )

        val expectedResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), INTERNAL_SERVER_ERROR)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(InternalServerDataRetrievalError(eisError.errorDetail.errorMessage))
    }

    "return MethodNotAllowedDataRetrievalError error when call to the integration framework fails with a METHOD_NOT_ALLOWED" in forAll {
      (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(errorCode = METHOD_NOT_ALLOWED)
        )

        val expectedResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), METHOD_NOT_ALLOWED)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(MethodNotAllowedDataRetrievalError(eisError.errorDetail.errorMessage))
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with invalid auth type error" in forAll {
      (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(
              errorCode = BAD_REQUEST,
              errorMessage = invalidAuthTypeEisErrorMessage
            )
        )

        val expectedResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), BAD_REQUEST)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(InternalServerDataRetrievalError("Invalid auth type UKNW"))
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with a unmanaged status code" in forAll {
      (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(errorCode = IM_A_TEAPOT)
        )

        val expectedResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), IM_A_TEAPOT)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(InternalServerDataRetrievalError(eisError.errorDetail.errorMessage))
    }

    "return UnableToDeserialiseDataRetrievalError error when call to the integration framework fails with unvalidated json" in forAll {
      (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val jsError = JsError(
          Seq((JsPath \ "errorDetail", Seq(JsonValidationError(JsonErrors.pathMissing))))
        )

        val expectedResponse = UpstreamErrorResponse("{}", BAD_REQUEST)

        when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
          .thenReturn(Future.failed(expectedResponse))

        val result = await(service.getAuthorisations(request).value)

        result shouldBe Left(UnableToDeserialiseDataRetrievalError(jsError))
    }
  }
}
