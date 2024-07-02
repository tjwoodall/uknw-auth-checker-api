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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, AuthorisationResponse, AuthorisationsResponse}

class AuthorisationsControllerSpec extends BaseSpec {

  val controller = new AuthorisationsController(Helpers.stubControllerComponents())

  "AuthorisationsController" should {

    "return OK (200) with authorised eoris when request has valid date and eoris" in {

      forAll { authorisationRequest: AuthorisationRequest =>
        val expectedResponse = AuthorisationsResponse(
          authorisationRequest.date,
          authorisationRequest.eoris.map(r => AuthorisationResponse(r, authorised = true))
        )

        val request = fakeRequestWithJsonBody(Json.toJson(authorisationRequest))

        val result = controller.authorisations()(request)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponse)
      }
    }
  }
}
