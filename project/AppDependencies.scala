import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % "2.12.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatestplus"      %% "scalacheck-1-18"        % "3.2.19.0"       % Test,
    "io.github.wolfendale"   %% "scalacheck-gen-regexp"  % "1.1.0"          % Test,
    "org.scalacheck"         %% "scalacheck"             % "1.18.0"         % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1"          % Test
  )

  val it: Seq[Nothing] = Seq.empty
}
