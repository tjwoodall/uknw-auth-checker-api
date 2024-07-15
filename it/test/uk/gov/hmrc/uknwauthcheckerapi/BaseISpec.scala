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
import uk.gov.hmrc.uknwauthcheckerapi.generators.{ExtensionHelpers, TestData, TestHeaders}

import scala.reflect.ClassTag

class BaseISpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with WireMockHelper
    with TestData
    with TestHeaders
    with ExtensionHelpers {

  @annotation.nowarn
  protected val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"              -> false,
    "auditing.enabled"             -> false,
    "http-verbs.retries.intervals" -> List("1ms", "1ms", "1ms"),
  ) ++ setWireMockPort(
    "auth",
    "integration-framework"
  )
  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(additionalAppConfig)
    .build()
  protected lazy val authorisationsUrl = s"http://localhost:$port/authorisations"
  protected val eisAuthorisationsEndpointPath = "/cau/validatecustomsauth/v1"
  private lazy val wsClient: WSClient = injected[WSClient]

  protected def injected[T](c: Class[T]): T = app.injector.instanceOf(c)
  protected def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  protected def deleteRequest(url: String, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).delete()
    )
  }

  protected def headRequest(url: String, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).head()
    )
  }

  protected def getRequest(url: String, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).get()
    )
  }

  protected def optionsRequest(url: String, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).options()
    )
  }

  protected def patchRequest(url: String, body: JsValue, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).patch(Json.toJson(body))
    )
  }

  protected def postRequest(url: String, body: JsValue, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).post(Json.toJson(body))
    )
  }

  protected def putRequest(url: String, body: JsValue, headers: Seq[(String, String)] = defaultHeaders): WSResponse = {
    await(wsClient.url(url)
      .addHttpHeaders(
        headers: _*
      ).put(Json.toJson(body))
    )
  }
}
