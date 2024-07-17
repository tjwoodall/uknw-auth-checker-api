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

import org.scalacheck.Arbitrary
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.eis._
import uk.gov.hmrc.uknwauthcheckerapi.utils.EisAuthTypes

import java.time.LocalDate

trait TestData extends Generators {

  protected val authorisationEndpoint = "authorisation"
  protected val emptyJson: JsValue = Json.parse("{}")

  protected val invalidAuthTypeEisErrorMessage: String = """Invalid authorisation type : UKNW""".stripMargin
  protected val invalidEorisEisErrorMessage:    String = """Invalid format of EORI(s): 0000000001,0000000003""".stripMargin
  protected val invalidDateEisErrorMessage:     String = """Invalid supplied date(Date format should be - YYYY-MM-DD) : 202-01-01""".stripMargin
  protected val invalidMixedEisErrorMessage: String =
    """Invalid format of EORI(s): 0000000001,0000000003,Invalid supplied date(Date format should be - YYYY-MM-DD) : 202-01-01""".stripMargin

  implicit protected val arbValidAuthorisationRequest: Arbitrary[ValidAuthorisationRequest] = Arbitrary {
    for {
      date  <- Arbitrary.arbitrary[LocalDate]
      eoris <- eorisGen
    } yield ValidAuthorisationRequest(
      AuthorisationRequest(
        date.toLocalDateFormatted,
        eoris
      )
    )
  }

  implicit protected val arbValidEisAuthorisationsResponse: Arbitrary[ValidEisAuthorisationsResponse] = Arbitrary {
    for {
      date  <- Arbitrary.arbitrary[LocalDate]
      eoris <- eorisGen
    } yield ValidEisAuthorisationsResponse(
      EisAuthorisationsResponse(
        date,
        EisAuthTypes.nopWaiver,
        eoris.map(e => EisAuthorisationResponse(e, valid = true, 0))
      )
    )
  }

  protected val badRequestEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = BAD_REQUEST,
        errorMessage = invalidMixedEisErrorMessage
      )
    )

  protected val forbiddenEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = FORBIDDEN,
        errorMessage = ""
      )
    )

  protected val imATeapotEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = IM_A_TEAPOT,
        errorMessage = ""
      )
    )

  protected val internalServerErrorEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = INTERNAL_SERVER_ERROR,
        errorMessage = ""
      )
    )

  protected val methodNotAllowedEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = METHOD_NOT_ALLOWED,
        errorMessage = ""
      )
    )
}

final case class ValidAuthorisationRequest(
  request: AuthorisationRequest
)

final case class ValidEisAuthorisationsResponse(
  response: EisAuthorisationsResponse
)
