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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, UpstreamErrorResponse}
import uk.gov.hmrc.uknwauthcheckerapi.config.AppConfig
import uk.gov.hmrc.uknwauthcheckerapi.models.Rfc7231DateTime
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{CustomHeaderNames, HmrcContentTypes}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationRequest, EisAuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.utils.{HeaderCarrierExtensions, RequestBuilderExtensions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject() (
  appConfig:                  AppConfig,
  httpClient:                 HttpClientV2,
  override val configuration: Config,
  override val actorSystem:   ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with RequestBuilderExtensions
    with HeaderCarrierExtensions {

  def getEisAuthorisationsResponse(eisAuthorisationRequest: EisAuthorisationRequest)(implicit hc: HeaderCarrier): Future[EisAuthorisationsResponse] =
    retryFor[EisAuthorisationsResponse]("Integration Framework Response")(retryCondition) {
      httpClient
        .post(appConfig.eisAuthorisationsUrl)
        .setHeader(integrationFrameworkHeaders(appConfig.integrationFrameworkBearerToken)*)
        .withBody(Json.toJson(eisAuthorisationRequest))
        .executeAndDeserialise[EisAuthorisationsResponse]
    }

  private def integrationFrameworkHeaders(bearerToken: String)(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      (CustomHeaderNames.xCorrelationId, generateCorrelationId()),
      (HeaderNames.DATE, Rfc7231DateTime.now),
      (HeaderNames.CONTENT_TYPE, HmrcContentTypes.json),
      (HeaderNames.ACCEPT, MimeTypes.JSON),
      (HeaderNames.AUTHORIZATION, s"Bearer $bearerToken")
    )

  private def retryCondition: PartialFunction[Exception, Boolean] = { case UpstreamErrorResponse.Upstream5xxResponse(_) =>
    true
  }
}
