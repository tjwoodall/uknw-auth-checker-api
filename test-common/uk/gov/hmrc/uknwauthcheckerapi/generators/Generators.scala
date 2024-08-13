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

import org.scalacheck.Gen
import wolfendale.scalacheck.regexp.RegexpGen

import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{CustomRegexes, MinMaxValues}

trait Generators {

  protected val minMaxValues: MinMaxValues

  private val eoriGen: Gen[String] = RegexpGen.from(CustomRegexes.eoriPattern)

  protected def eoriGenerator(min: Int = minMaxValues.minEoriCount, max: Int = minMaxValues.maxEoriCount): Gen[Seq[String]] =
    Gen.chooseNum(min, max).flatMap(n => Gen.listOfN(n, eoriGen))
}
