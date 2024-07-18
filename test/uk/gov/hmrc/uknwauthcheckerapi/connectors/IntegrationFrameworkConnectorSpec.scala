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
import uk.gov.hmrc.uknwauthcheckerapi.generators.ValidEisAuthorisationsResponse
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.utils.JsonErrors

class IntegrationFrameworkConnectorSpec extends BaseSpec {

  private val callAmountWithRetries = 4

  private lazy val connector: IntegrationFrameworkConnector = injected[IntegrationFrameworkConnector]

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "getEisAuthorisationsResponse" should {
    "return EisAuthorisationsResponse when call to integration framework succeeds" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest, validGetAuthorisationsResponse: ValidEisAuthorisationsResponse) =>
        whenever(eisAuthorisationRequest.validityDate.isDefined) {
          beforeEach()

          when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

          doReturn(
            Future.successful(
              HttpResponse.apply(OK, Json.stringify(Json.toJson(validGetAuthorisationsResponse.response)))
            )
          ).when(mockRequestBuilder).execute[HttpResponse](any(), any())

          val result = await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))

          result shouldBe validGetAuthorisationsResponse.response
        }
    }

    "return ERROR when call to integration framework succeeds but deserialisation fails" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        whenever(eisAuthorisationRequest.validityDate.isDefined) {
          beforeEach()

          val expectedError = JsError(
            Seq("authType", "processingDate", "results").map { field =>
              (JsPath \ field, Seq(JsonValidationError(JsonErrors.pathMissing)))
            }
          )

          when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

          doReturn(
            Future.successful(
              HttpResponse.apply(OK, "{}")
            )
          ).when(mockRequestBuilder).execute[HttpResponse](any(), any())

          Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
            case Failure(exception: JsResult.Exception) => exception.cause shouldBe expectedError
            case Success(_)                             => fail("expected exception to be thrown")
            case _                                      => fail("unexpected response")
          }
        }
    }

    "return 400 BAD_REQUEST UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "BAD_REQUEST")))

        Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual BAD_REQUEST
          case _ => fail("expected UpstreamErrorResponse when error is received")
        }
    }

    "return 403 FORBIDDEN UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(FORBIDDEN, "FORBIDDEN")))

        Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual FORBIDDEN
          case _ => fail("expected UpstreamErrorResponse when error is received")
        }
    }

    "return 405 METHOD_NOT_ALLOWED UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED")))

        Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual METHOD_NOT_ALLOWED
          case _ => fail("expected UpstreamErrorResponse when error is received")
        }
    }

    "return 500 INTERNAL_SERVER_ERROR UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")))

        Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual INTERNAL_SERVER_ERROR
          case _ => fail("expected UpstreamErrorResponse when error is received")
        }

        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute(any(), any())
    }

    "return 502 SERVICE_UNAVAILABLE UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE")))

        Try(await(connector.getEisAuthorisationsResponse(eisAuthorisationRequest))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual SERVICE_UNAVAILABLE
          case _ => fail("expected UpstreamErrorResponse when error is received")
        }

        verify(mockRequestBuilder, times(callAmountWithRetries))
          .execute(any(), any())
    }
  }
}
