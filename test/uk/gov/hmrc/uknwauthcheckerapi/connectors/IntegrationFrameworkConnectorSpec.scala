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

package uk.gov.hmrc.uknwauthcheckerapi.connectors

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.http.Status._
import play.api.libs.json._
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.generators.{TestConstants, ValidEisAuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{ApiErrorCodes, JsonErrorMessages, JsonPaths}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationRequest, EisAuthorisationsResponse}

class IntegrationFrameworkConnectorSpec extends BaseSpec {

  private lazy val connector: IntegrationFrameworkConnector = injected[IntegrationFrameworkConnector]

  trait TestContext {
    def doTest(request: EisAuthorisationRequest, statusCode: Int, jsonBody: String): EisAuthorisationsResponse = {
      reset(mockHttpClient)
      reset(mockRequestBuilder)

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(using any(), any(), any())).thenReturn(mockRequestBuilder)

      doReturn(
        Future.successful(
          HttpResponse.apply(statusCode, jsonBody)
        )
      ).when(mockRequestBuilder).execute[HttpResponse](using any(), any())

      await(connector.getEisAuthorisationsResponse(request))
    }
  }

  "getEisAuthorisationsResponse" should {
    "return EisAuthorisationsResponse when call to integration framework succeeds" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest, validGetAuthorisationsResponse: ValidEisAuthorisationsResponse) =>
        whenever(eisAuthorisationRequest.validityDate.isDefined) {

          val response = doTest(
            eisAuthorisationRequest,
            OK,
            Json.stringify(Json.toJson(validGetAuthorisationsResponse.response))
          )

          response shouldBe validGetAuthorisationsResponse.response
        }
      }
    }

    "return ERROR when call to integration framework succeeds but deserialisation fails" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        whenever(eisAuthorisationRequest.validityDate.isDefined) {

          val expectedError = JsError(
            Seq(JsonPaths.results, JsonPaths.authType, JsonPaths.processingDate).map { field =>
              (JsPath \ field, Seq(JsonValidationError(JsonErrorMessages.pathMissing)))
            }
          )

          Try(
            doTest(
              eisAuthorisationRequest,
              OK,
              TestConstants.emptyJson
            )
          ) match {
            case Failure(exception: JsResult.Exception) => exception.cause shouldBe expectedError
            case Success(_)                             => fail(TestConstants.errorExpectedException)
            case _                                      => fail(TestConstants.errorUnexpectedResponse)
          }
        }
      }
    }

    "return 400 BAD_REQUEST UpstreamErrorResponse when call to integration framework returns an error" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        Try(
          doTest(
            eisAuthorisationRequest,
            BAD_REQUEST,
            ApiErrorCodes.badRequest
          )
        ) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) => code shouldEqual BAD_REQUEST
          case _                                             => fail(TestConstants.errorExpectedUpstreamResponse)
        }
      }
    }

    "return 403 FORBIDDEN UpstreamErrorResponse when call to integration framework returns an error" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        Try(
          doTest(
            eisAuthorisationRequest,
            FORBIDDEN,
            ApiErrorCodes.forbidden
          )
        ) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) => code shouldEqual FORBIDDEN
          case _                                             => fail(TestConstants.errorExpectedUpstreamResponse)
        }
      }
    }

    "return 405 METHOD_NOT_ALLOWED UpstreamErrorResponse when call to integration framework returns an error" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        Try(
          doTest(
            eisAuthorisationRequest,
            METHOD_NOT_ALLOWED,
            ApiErrorCodes.methodNotAllowed
          )
        ) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual METHOD_NOT_ALLOWED
          case _ => fail(TestConstants.errorExpectedUpstreamResponse)
        }
      }
    }

    "return 500 INTERNAL_SERVER_ERROR UpstreamErrorResponse when call to integration framework returns an error" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        Try(
          doTest(
            eisAuthorisationRequest,
            INTERNAL_SERVER_ERROR,
            ApiErrorCodes.internalServerError
          )
        ) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual INTERNAL_SERVER_ERROR
          case _ => fail(TestConstants.errorExpectedUpstreamResponse)
        }

        verify(mockRequestBuilder, times(TestConstants.callAmountWithRetries))
          .execute(using any(), any())
      }
    }

    "return 502 SERVICE_UNAVAILABLE UpstreamErrorResponse when call to integration framework returns an error" in new TestContext {
      forAll { (eisAuthorisationRequest: EisAuthorisationRequest) =>
        Try(
          doTest(
            eisAuthorisationRequest,
            SERVICE_UNAVAILABLE,
            ApiErrorCodes.serviceUnavailable
          )
        ) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual SERVICE_UNAVAILABLE
          case _ => fail(TestConstants.errorExpectedUpstreamResponse)
        }

        verify(mockRequestBuilder, times(TestConstants.callAmountWithRetries))
          .execute(using any(), any())
      }
    }
  }
}
