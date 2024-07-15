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
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationRequest, EisAuthorisationResponseError, EisAuthorisationResponseErrorDetail}
import uk.gov.hmrc.uknwauthcheckerapi.utils.{CustomRegexes, EisAuthTypes}
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.LocalDate

trait Generators extends ExtensionHelpers {

  protected val eoriGen:  Gen[String]      = RegexpGen.from(CustomRegexes.eoriPattern)
  protected val eorisGen: Gen[Seq[String]] = Gen.chooseNum(1, 3000).flatMap(n => Gen.listOfN(n, eoriGen))

  implicit protected val arbLocalDate: Arbitrary[LocalDate] = Arbitrary(
    Gen
      .choose(
        min = LocalDate.MIN.toEpochDay,
        max = LocalDate.MAX.toEpochDay
      )
      .map(LocalDate.ofEpochDay)
  )

  implicit protected val arbAuthorisationRequest: Arbitrary[AuthorisationRequest] = Arbitrary {
    for {
      date  <- Arbitrary.arbitrary[LocalDate]
      eoris <- eorisGen
    } yield AuthorisationRequest(date.toLocalDateFormatted, eoris)
  }

  implicit protected val arbEisAuthorisationRequest: Arbitrary[EisAuthorisationRequest] = Arbitrary {
    for {
      localDate  <- Arbitrary.arbitrary[LocalDate]
      dateOption <- Gen.option(localDate)
      eoris      <- eorisGen
    } yield EisAuthorisationRequest(dateOption, EisAuthTypes.NopWaiver, eoris)
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

}
