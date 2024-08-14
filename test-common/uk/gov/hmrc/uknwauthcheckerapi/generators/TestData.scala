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

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import org.scalacheck.Gen.chooseNum
import org.scalacheck.{Arbitrary, Gen}

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.MinMaxValues
import uk.gov.hmrc.uknwauthcheckerapi.models.eis._

trait TestData extends Generators {

  protected val minMaxValues: MinMaxValues

  protected val emptyJson: JsValue = Json.parse(TestConstants.emptyJson)

  implicit protected val arbAuthorisationRequest: Arbitrary[AuthorisationRequest] = Arbitrary {
    for {
      eoris <- eoriGenerator()
    } yield AuthorisationRequest(eoris)
  }

  implicit protected val arbEisAuthorisationRequest: Arbitrary[EisAuthorisationRequest] = Arbitrary {
    for {
      localDate  <- Arbitrary.arbitrary[LocalDate]
      dateOption <- Gen.option(localDate)
      eoris      <- eoriGenerator()
    } yield EisAuthorisationRequest(dateOption, EisAuthTypes.nopWaiver, eoris)
  }

  implicit protected val arbEisAuthorisationResponseError: Arbitrary[EisAuthorisationResponseError] = Arbitrary {
    for {
      errorCode    <- Arbitrary.arbitrary[Int]
      errorMessage <- Arbitrary.arbitrary[String]
    } yield EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = errorCode,
        errorMessage = errorMessage
      )
    )
  }

  implicit protected val arbInvalidEorisAuthorisationRequest: Arbitrary[InvalidEorisAuthorisationRequest] = Arbitrary {
    for {
      randomString <- Arbitrary.arbitrary[String].filterNot(_.isEmpty)
    } yield InvalidEorisAuthorisationRequest(
      AuthorisationRequest(
        Seq(randomString)
      )
    )
  }

  implicit protected val arbLocalDate: Arbitrary[LocalDate] = Arbitrary(
    Gen
      .choose(
        min = LocalDate.MIN.toEpochDay,
        max = LocalDate.MAX.toEpochDay
      )
      .map(LocalDate.ofEpochDay)
  )

  implicit lazy val arbLocalDateTime: Arbitrary[LocalDateTime] = {
    import java.time.ZoneOffset.UTC
    Arbitrary {
      for {
        seconds <- chooseNum(LocalDateTime.MIN.toEpochSecond(UTC), LocalDateTime.MAX.toEpochSecond(UTC))
        nanos   <- chooseNum(LocalDateTime.MIN.getNano, LocalDateTime.MAX.getNano)
      } yield LocalDateTime.ofEpochSecond(seconds, nanos, UTC)
    }
  }

  implicit protected val arbNoEorisAuthorisationRequest: Arbitrary[NoEorisAuthorisationRequest] = Arbitrary {
    NoEorisAuthorisationRequest(
      AuthorisationRequest(
        Seq.empty
      )
    )
  }

  implicit protected val arbTooManyEorisAuthorisationRequest: Arbitrary[TooManyEorisAuthorisationRequest] = Arbitrary {
    for {
      eoris <- eoriGenerator(minMaxValues.maxEoriCount + 1, minMaxValues.maxEoriCount + 5)
    } yield TooManyEorisAuthorisationRequest(
      AuthorisationRequest(
        eoris
      )
    )
  }

  implicit protected val arbValidAuthorisationRequest: Arbitrary[ValidAuthorisationRequest] = Arbitrary {
    for {
      eoris <- eoriGenerator()
    } yield ValidAuthorisationRequest(
      AuthorisationRequest(
        eoris
      )
    )
  }

  implicit protected val arbValidEisAuthorisationsResponse: Arbitrary[ValidEisAuthorisationsResponse] = Arbitrary {
    for {
      dateTime <- Arbitrary.arbitrary[ZonedDateTime]
      eoris    <- eoriGenerator()
    } yield ValidEisAuthorisationsResponse(
      EisAuthorisationsResponse(
        dateTime,
        EisAuthTypes.nopWaiver,
        eoris.map(e => EisAuthorisationResponse(e, valid = true, 0))
      )
    )
  }

  protected val badRequestEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = BAD_REQUEST,
        errorMessage = TestConstants.invalidEorisEisErrorMessage
      )
    )

  protected val forbiddenEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = FORBIDDEN,
        errorMessage = TestConstants.emptyString
      )
    )

  protected val imATeapotEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = IM_A_TEAPOT,
        errorMessage = TestConstants.emptyString
      )
    )

  protected val methodNotAllowedEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = METHOD_NOT_ALLOWED,
        errorMessage = TestConstants.emptyString
      )
    )

  protected val internalServerErrorEisAuthorisationResponseError: EisAuthorisationResponseError =
    EisAuthorisationResponseError(
      errorDetail = EisAuthorisationResponseErrorDetail(
        errorCode = INTERNAL_SERVER_ERROR,
        errorMessage = TestConstants.emptyString
      )
    )
}

final case class InvalidEorisAuthorisationRequest(request: AuthorisationRequest)
final case class NoEorisAuthorisationRequest(request: AuthorisationRequest)
final case class TooManyEorisAuthorisationRequest(request: AuthorisationRequest)
final case class ValidAuthorisationRequest(request: AuthorisationRequest)
final case class ValidEisAuthorisationsResponse(response: EisAuthorisationsResponse)
