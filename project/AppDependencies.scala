import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.0.0"
  
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30" % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"    % bootstrapVersion  % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"           % "3.2.18.0"        % Test,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"     % "1.1.0"           % Test,
    "org.scalacheck"          %% "scalacheck"                % "1.18.0"          % Test,
  )

  val it: Seq[Nothing] = Seq.empty
}
