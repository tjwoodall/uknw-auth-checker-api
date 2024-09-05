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

package uk.gov.hmrc.uknwauthcheckerapi.services

import java.time.{ZoneId, ZonedDateTime}
import javax.inject.Singleton

import uk.gov.hmrc.uknwauthcheckerapi.models.Iso8601DateTimeFormatter

@Singleton
class ZonedDateTimeService {
  def nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))

  def nowAsIsoUtc8601String(): String = Iso8601DateTimeFormatter.format(nowUtc())
}
