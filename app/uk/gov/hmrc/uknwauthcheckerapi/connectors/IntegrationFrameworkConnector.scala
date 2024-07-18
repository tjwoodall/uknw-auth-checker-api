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

package uk.gov.hmrc.uknwauthcheckerapi.connectors

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}
import uk.gov.hmrc.uknwauthcheckerapi.config.AppConfig
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationRequest, EisAuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.models.{CustomHeaderNames, RFC7231DateTime}
import uk.gov.hmrc.uknwauthcheckerapi.utils.HmrcContentTypes

@Singleton
class IntegrationFrameworkConnector @Inject() (
  appConfig:                  AppConfig,
  httpClient:                 HttpClientV2,
  override val configuration: Config,
  override val actorSystem:   ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with BaseConnector {

  private def integrationFrameworkHeaders(bearerToken: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val correlationId = hc.headers(scala.Seq(CustomHeaderNames.xCorrelationId)) match {
      case Seq((_, id)) =>
        id
      case _ =>
        UUID.randomUUID().toString
    }
    Seq(
      (CustomHeaderNames.xCorrelationId, correlationId),
      (HeaderNames.DATE, RFC7231DateTime.now),
      (HeaderNames.CONTENT_TYPE, HmrcContentTypes.json),
      (HeaderNames.ACCEPT, MimeTypes.JSON),
      (HeaderNames.AUTHORIZATION, s"Bearer $bearerToken")
    )
  }

  def getEisAuthorisationsResponse(eisAuthorisationRequest: EisAuthorisationRequest)(implicit hc: HeaderCarrier): Future[EisAuthorisationsResponse] =
    retryFor[EisAuthorisationsResponse]("Integration framework Response")(retryCondition) {
      httpClient
        .post(url"${appConfig.baseUrl("integration-framework")}/cau/validatecustomsauth/v1")
        .setHeader(integrationFrameworkHeaders(appConfig.integrationFrameworkBearerToken): _*)
        .withBody(Json.toJson(eisAuthorisationRequest))
        .executeAndDeserialise[EisAuthorisationsResponse]
    }
}
