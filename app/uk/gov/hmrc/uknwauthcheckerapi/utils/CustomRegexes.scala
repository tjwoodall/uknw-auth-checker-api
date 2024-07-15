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

package uk.gov.hmrc.uknwauthcheckerapi.utils

import scala.util.matching.Regex

object CustomRegexes {
  val eoriPattern:                 String = "^(GB|XI)[0-9]{12}|(GB|XI)[0-9]{15}$"
  val invalidAuthTypePattern:      String = "^.*(Invalid authorisation type).*$"
  val invalidFormatOfDatePattern:  String = "^.*(supplied date).*$"
  val invalidFormatOfEorisPattern: String = "^.*(format of EORI).*$"
  val uuid:                        String = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"

  val invalidAuthTypePatternRegex: Regex = invalidAuthTypePattern.r
}
