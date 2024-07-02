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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.uknwauthcheckerapi.generators.Generators

import scala.reflect.ClassTag

class BaseISpec extends PlaySpec with GuiceOneServerPerSuite
                                  with Generators {

  lazy val hostUrl: String = s"http://localhost:$port"
  lazy val authorisationsUrl = s"$hostUrl/authorisations"
  override lazy val app: Application = GuiceApplicationBuilder().build()
  private lazy val wsClient: WSClient = injected[WSClient]

  def injected[T](c: Class[T]): T                    = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  def postRequestWithHeader(url: String, body: JsValue, headers: Seq[(String, String)]): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).post(Json.toJson(body))
    )
  }

  def postRequestWithoutHeader(url: String, body: JsValue): WSResponse = {
    await(wsClient.url(url).post(Json.toJson(body)))
  }

}
