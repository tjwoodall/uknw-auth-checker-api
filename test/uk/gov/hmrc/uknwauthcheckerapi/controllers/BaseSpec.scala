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

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, HttpVerbs}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.POST
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.uknwauthcheckerapi.config.AppConfig
import uk.gov.hmrc.uknwauthcheckerapi.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.uknwauthcheckerapi.generators.{ExtensionHelpers, Generators, TestData, TestHeaders}
import uk.gov.hmrc.uknwauthcheckerapi.services.{IntegrationFrameworkService, ValidationService}

import scala.concurrent.{ExecutionContext, Future}
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

  implicit lazy val ec:           ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc:           HeaderCarrier    = HeaderCarrier()
  implicit lazy val system:       ActorSystem      = ActorSystem()
  implicit lazy val materializer: Materializer     = Materializer(system)

  @annotation.nowarn
  protected val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"              -> false,
    "auditing.enabled"             -> false,
    "http-verbs.retries.intervals" -> List("1ms", "1ms", "1ms")
  ) ++ configOverrides
  protected val actorSystem:                            ActorSystem                         = ActorSystem("actor")
  protected lazy val appConfig:                         AppConfig                           = injected[AppConfig]
  protected lazy val config:                            Config                              = injected[Config]
  protected val fakePostRequest:                        FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, "")
  protected lazy val mockAuthConnector:                 AuthConnector                       = mock[AuthConnector]
  protected lazy val mockHttpClient:                    HttpClientV2                        = mock[HttpClientV2]
  protected lazy val mockRequestBuilder:                RequestBuilder                      = mock[RequestBuilder]
  protected lazy val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector       = mock[IntegrationFrameworkConnector]
  protected lazy val mockIntegrationFrameworkService:   IntegrationFrameworkService         = mock[IntegrationFrameworkService]
  protected lazy val mockValidationService:             ValidationService                   = mock[ValidationService]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .overrides(moduleOverrides)
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

  def moduleOverrides: AbstractModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[ActorSystem]).toInstance(actorSystem)
      bind(classOf[AuthConnector]).toInstance(mockAuthConnector)
      bind(classOf[ExecutionContext]).toInstance(ec)
      bind(classOf[ControllerComponents]).toInstance(Helpers.stubControllerComponents())
      bind(classOf[HttpClientV2]).toInstance(mockHttpClient)
      bind(classOf[RequestBuilder]).toInstance(mockRequestBuilder)
    }
  }

  protected def stubAuthorization(): Unit = {
    val retrievalResult = Future.successful(Credentials("id", "StandardApplication"))

    when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
      .thenReturn(retrievalResult)

    when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(any(), any()))
      .thenReturn(Future.successful(()))
  }
}
