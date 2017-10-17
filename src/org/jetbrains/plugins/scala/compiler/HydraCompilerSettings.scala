package org.jetbrains.plugins.scala.compiler

import java.io.File

import com.intellij.openapi.components._
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.compiler.SourcePartitioner.AUTO

import scala.beans.BeanProperty

/**
  * @author Maris Alexandru
  */
@State(
        name = "HydraSettings",
        storages = Array(new Storage("hydra.xml"))
)
class HydraCompilerSettings(project: Project) extends PersistentStateComponent[HydraCompilerSettingsState] {

  var isHydraEnabled: Boolean = false

  var hydraVersion: String = ""

  var noOfCores: String = Math.ceil(Runtime.getRuntime.availableProcessors()/2D).toInt.toString

  var hydraStorePath: String = getDefaultHydraStorePath

  var sourcePartitioner: String = AUTO.value

  private val ProjectRoot: String = project.getPresentableUrl

  override def getState: HydraCompilerSettingsState = {
    val state = new HydraCompilerSettingsState()
    state.hydraVersion = hydraVersion
    state.noOfCores = noOfCores
    state.isHydraEnabled = isHydraEnabled
    state.hydraStorePath = hydraStorePath + File.separator
    state.sourcePartitioner = sourcePartitioner
    state.projectRoot = ProjectRoot
    state
  }

  override def loadState(state: HydraCompilerSettingsState): Unit = {
    isHydraEnabled = state.isHydraEnabled
    hydraVersion = state.hydraVersion
    noOfCores = state.noOfCores
    hydraStorePath = state.hydraStorePath
    sourcePartitioner = state.sourcePartitioner
  }

  def getDefaultHydraStorePath: String = project.getPresentableUrl + File.separator + ".hydra"
}

object HydraCompilerSettings {
  def getInstance(project: Project): HydraCompilerSettings = ServiceManager.getService(project, classOf[HydraCompilerSettings])
}

class HydraCompilerSettingsState {
  @BeanProperty
  var isHydraEnabled: Boolean = false

  @BeanProperty
  var hydraVersion: String = ""

  @BeanProperty
  var noOfCores: String = ""

  @BeanProperty
  var hydraStorePath: String = ""

  @BeanProperty
  var sourcePartitioner: String = ""

  @BeanProperty
  var projectRoot: String = ""
}

object SourcePartitioner {
  sealed abstract class SourcePartitioner(val value: String)

  case object AUTO extends SourcePartitioner("auto")
  case object EXPLICIT extends SourcePartitioner("explicit")
  case object PLAIN extends SourcePartitioner("plain")
  case object PACKAGE extends SourcePartitioner("package")

  val values: Seq[SourcePartitioner] = Seq(AUTO, EXPLICIT, PLAIN, PACKAGE)
}
