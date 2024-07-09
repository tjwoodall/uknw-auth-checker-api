/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, reset, when}
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.generators.ValidGetAuthorisationsResponse
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationRequest

import scala.concurrent.Future

class IntegrationFrameworkConnectorSpec extends BaseSpec {

  val retryAmount = 4
  val mockHttpClient:     HttpClientV2   = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val connector = new IntegrationFrameworkConnector(appConfig, mockHttpClient, config, actorSystem)

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "getEisAuthorisationsResponse" should {
    "return response when call to integration framework succeeds" in forAll {
      (eisAuthorisationRequest: EisAuthorisationRequest, validGetAuthorisationsResponse: ValidGetAuthorisationsResponse) =>
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
  }
}
