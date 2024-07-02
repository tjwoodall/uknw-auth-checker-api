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

package uk.gov.hmrc.uknwauthcheckerapi

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest

class AuthorisationControllerISpec extends BaseISpec {

  "POST /authorisations" should {
    "return OK (200) with authorised eoris when request has valid date and eoris" in {
      forAll { authorisationRequest: AuthorisationRequest =>
        val authorisationRequestJson = Json.toJson(authorisationRequest)

        postRequestWithoutHeader(authorisationsUrl, authorisationRequestJson).status mustBe Status.OK
      }
    }
  }
}

