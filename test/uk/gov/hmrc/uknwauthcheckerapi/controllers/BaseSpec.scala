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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsValue
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers.POST
import uk.gov.hmrc.uknwauthcheckerapi.generators.Generators

class BaseSpec extends AnyWordSpec with Matchers with Generators {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")

  implicit lazy val system:       ActorSystem  = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)

  def fakeRequestWithJsonBody(json: JsValue): FakeRequest[JsValue] = FakeRequest(POST, "/authorisations", FakeHeaders(headers), json)
}
