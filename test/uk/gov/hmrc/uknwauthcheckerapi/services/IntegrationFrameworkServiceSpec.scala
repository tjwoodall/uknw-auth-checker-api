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

package uk.gov.hmrc.uknwauthcheckerapi.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.test.Helpers.await
import uk.gov.hmrc.uknwauthcheckerapi.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, AuthorisationResponse, AuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationResponse, EisAuthorisationsResponse}

import java.time.LocalDate
import scala.concurrent.Future

class IntegrationFrameworkServiceSpec extends BaseSpec {

  private val mockIntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]

  val service = new IntegrationFrameworkService(appConfig, mockIntegrationFrameworkConnector)(ec)

  "getEisAuthorisations" should {
    "return successful Eis Authorisations response when call to the integration framework succeeds" in forAll {
      authorisationRequest: AuthorisationRequest =>
        whenever(authorisationRequest.date.isDefined) {
          val expectedResponse = AuthorisationsResponse(
            authorisationRequest.date.getOrElse(LocalDate.now),
            authorisationRequest.eoris.map(r => AuthorisationResponse(r, authorised = true))
          )
          val expectedEisAuthorisationsResponse = EisAuthorisationsResponse(
            authorisationRequest.date.getOrElse(LocalDate.now),
            appConfig.authType,
            authorisationRequest.eoris.map(r => EisAuthorisationResponse(r, valid = true, 0))
          )
          when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
            .thenReturn(Future.successful(expectedEisAuthorisationsResponse))

          val result = await(service.getAuthorisations(authorisationRequest))

          result shouldBe expectedResponse
        }
    }
  }
}
