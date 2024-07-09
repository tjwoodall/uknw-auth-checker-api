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

package uk.gov.hmrc.uknwauthcheckerapi.generators

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationResponse, EisAuthorisationsResponse}
import java.time.LocalDate

trait TestData extends Generators {

  val authorisationEndpoint = "authorisation"
  val emptyJson: JsValue = Json.parse("{}")

  def randomAuthorisationRequest: AuthorisationRequest = arbAuthorisationRequest.arbitrary.sample.get

  implicit val arbValidGetAuthorisationsResponse: Arbitrary[ValidGetAuthorisationsResponse] = Arbitrary {
    for {
      date  <- Gen.option(LocalDate.now())
      eoris <- eorisGen
    } yield ValidGetAuthorisationsResponse(
      EisAuthorisationsResponse(
        date.getOrElse(LocalDate.now()),
        "UKNW",
        eoris.map(e => EisAuthorisationResponse(e, valid = true, 0))
      )
    )

  }
}

final case class ValidGetAuthorisationsResponse(
  response: EisAuthorisationsResponse
)
