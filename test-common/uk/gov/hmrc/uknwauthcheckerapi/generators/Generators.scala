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
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationRequest
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.LocalDate

trait Generators {

  val eoriGen:  Gen[String]      = RegexpGen.from("^(GB|XI)[0-9]{12}|(GB|XI)[0-9]{15}$")
  val eorisGen: Gen[Seq[String]] = Gen.chooseNum(1, 3000).flatMap(n => Gen.listOfN(n, eoriGen))

  implicit val arbAuthorisationRequest: Arbitrary[AuthorisationRequest] = Arbitrary {
    for {
      date  <- Gen.option(LocalDate.now())
      eoris <- eorisGen
    } yield AuthorisationRequest(date, eoris)
  }

  implicit val arbEisAuthorisationRequest: Arbitrary[EisAuthorisationRequest] = Arbitrary {
    for {
      date  <- Gen.option(LocalDate.now())
      eoris <- eorisGen
    } yield EisAuthorisationRequest(date, "UKNW", eoris)
  }

}
