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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, HttpVerbs}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.POST
import uk.gov.hmrc.uknwauthcheckerapi.config.AppConfig
import uk.gov.hmrc.uknwauthcheckerapi.generators.{ExtensionHelpers, Generators, TestData, TestHeaders}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class BaseSpec
    extends AnyWordSpec
    with Matchers
    with Generators
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with DefaultAwaitTimeout
    with HeaderNames
    with TestData
    with TestHeaders
    with ExtensionHelpers {

  implicit val ec:                ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc:                HeaderCarrier    = HeaderCarrier()
  implicit lazy val system:       ActorSystem      = ActorSystem()
  implicit lazy val materializer: Materializer     = Materializer(system)

  @annotation.nowarn
  protected val additionalAppConfig: Map[String, Any] = Map(
    "create-internal-auth-token-on-start" -> false,
    "metrics.enabled"                     -> false,
    "auditing.enabled"                    -> false,
    "http-verbs.retries.intervals"        -> List("1ms", "1ms", "1ms")
  ) ++ configOverrides
  protected val actorSystem:     ActorSystem                         = ActorSystem("actor")
  protected lazy val appConfig:  AppConfig                           = injected[AppConfig]
  protected lazy val config:     Config                              = injected[Config]
  protected val fakePostRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, "")
  protected val stubComponents:  ControllerComponents                = Helpers.stubControllerComponents()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .build()

  protected def configOverrides: Map[String, Any] = Map()

  protected def injected[T](c:                 Class[T]):    T = app.injector.instanceOf(c)
  protected def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  protected def fakeRequestWithJsonBody(
    json:    JsValue,
    verb:    String = HttpVerbs.POST,
    headers: Seq[(String, String)] = defaultHeaders
  ): FakeRequest[JsValue] =
    FakeRequest(verb, authorisationEndpoint, FakeHeaders(headers), json)
}
