package org.jetbrains.plugins.scala.compiler

import java.awt.event.{ActionEvent, FocusEvent, FocusListener}
import javax.swing.event.DocumentEvent

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.{Messages, TextComponentAccessor}
import org.jetbrains.plugins.scala.caches.HydraArtifactsCache
import org.jetbrains.plugins.scala.extensions
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import org.jetbrains.plugins.scala.project.{ProjectExt, Version, Versions}
import org.jetbrains.plugins.scala.settings.HydraApplicationSettings

import scala.util.{Failure, Success}

/**
  * @author Maris Alexandru
  */
class ScalaHydraCompilerConfigurationPanel(project: Project, settings: HydraCompilerSettings, hydraGlobalSettings: HydraApplicationSettings) extends HydraCompilerConfigurationPanel {

  private val fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false)

  private val documentAdapter = new DocumentAdapter {
    override def textChanged(documentEvent: DocumentEvent): Unit = downloadButton.setEnabled(getUsername.nonEmpty && getPassword.nonEmpty)
  }

  private val focusListener = new FocusListener {
    override def focusGained(e: FocusEvent): Unit = {}

    override def focusLost(e: FocusEvent): Unit = if (getUsername.nonEmpty && getPassword.nonEmpty &&
      (HydraCredentialsManager.getLogin != getUsername || HydraCredentialsManager.getPlainPassword != getPassword)) {
      HydraCredentialsManager.setCredentials(getUsername, getPassword)
      hydraVersionComboBox.setItems(downloadHydraVersions)
    }
  }

  hydraGlobalSettings.getState
  userTextField.addFocusListener(focusListener)
  userTextField.getDocument.addDocumentListener(documentAdapter)
  passwordTextField.getDocument.addDocumentListener(documentAdapter)
  passwordTextField.addFocusListener(focusListener)
  hydraVersionComboBox.setItems(downloadHydraVersions)
  downloadButton.addActionListener((_: ActionEvent) => onDownload())
  noOfCoresComboBox.setItems(Array.range(1, Runtime.getRuntime.availableProcessors()/2 + 1).map(_.toString))
  sourcePartitionerComboBox.setItems(SourcePartitioner.values.map(_.value).toArray)

  hydraStoreDirectoryField.addBrowseFolderListener("", "Hydra Store Path", null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)
  hydraStoreDirectoryField.setText(settings.getDefaultHydraStorePath)

  hydraStoreDirectoryField.getTextField.getDocument.addDocumentListener(new DocumentAdapter() {
    override protected def textChanged(e: DocumentEvent): Unit = {
      hydraStoreDirectoryField.getTextField.setForeground(if (hydraStoreDirectoryField.getText == settings.getDefaultHydraStorePath) getDefaultValueColor
      else getChangedValueColor)
    }
  })

  def selectedVersion: String = hydraVersionComboBox.getSelectedItem.toString

  def setSelectedVersion(version: String): Unit = hydraVersionComboBox.setSelectedItem(version)

  def selectedNoOfCores: String = noOfCoresComboBox.getSelectedItem.toString

  def setSelectedNoOfCores(numberOfCores: String): Unit = noOfCoresComboBox.setSelectedItem(numberOfCores)

  def selectedSourcePartitioner: String = sourcePartitionerComboBox.getSelectedItem.toString

  def setSelectedSourcePartitioner(sourcePartitioner: String): Unit = sourcePartitionerComboBox.setSelectedItem(sourcePartitioner)

  def getHydraStoreDirectory: String = hydraStoreDirectoryField.getText

  def setHydraStoreDirectory(path: String): Unit = hydraStoreDirectoryField.setText(path)

  def onDownload(): Unit = {
    val scalaVersions = for {
      module <- project.scalaModules
      scalaVersion <- module.sdk.compilerVersion
    } yield scalaVersion

    downloadVersionWithProgress(scalaVersions, selectedVersion)
    settings.hydraVersion = selectedVersion
  }

  private def downloadHydraVersions = {
    (Versions.loadHydraVersions ++ hydraGlobalSettings.getDownloadedHydraVersions)
      .distinct
      .sortWith(Version(_) >= Version(_))
  }

  private def downloadVersionWithProgress(scalaVersions: Seq[String], hydraVersion: String): Unit = {
    val filteredScalaVersions = for {
      rawVersion <- scalaVersions.distinct
      if rawVersion != "2.12.0"
      version = Version(rawVersion)
      if version >= Version("2.11")
      filteredVersion = version.presentation
    } yield filteredVersion

    val filteredScalaVersionsString = filteredScalaVersions.mkString(", ")
    val scalaVersionsToBeDownloaded = filteredScalaVersions.filterNot(hydraGlobalSettings.artifactPaths.contains(_, hydraVersion))
    val scalaVersionsToBeDownloadedString = scalaVersionsToBeDownloaded.mkString(", ")
    if (scalaVersionsToBeDownloaded.nonEmpty) {
      val result = extensions.withProgressSynchronouslyTry(s"Downloading Hydra $hydraVersion for $scalaVersionsToBeDownloadedString")(downloadVersion(scalaVersionsToBeDownloaded, hydraVersion))
      result match {
        case Failure(exception) => {
          Messages.showErrorDialog(contentPanel, exception.getMessage, s"Error Downloading Hydra $hydraVersion for $scalaVersionsToBeDownloadedString")
        }
        case Success(_) => Messages.showInfoMessage(s"Successfully downloaded Hydra $hydraVersion for $scalaVersionsToBeDownloadedString", "Download Hydra Successful")
      }
    } else {
      Messages.showInfoMessage(s"Hydra $hydraVersion for $filteredScalaVersionsString is already downloaded", "Hydra version already downloaded")
    }
  }

  private def downloadVersion(scalaVersions: Seq[String], hydraVersion: String): (String => Unit) => Unit =
    (listener: (String) => Unit) => scalaVersions.foreach(version => HydraArtifactsCache.downloadIfNotPresent(version, hydraVersion, listener))

}
