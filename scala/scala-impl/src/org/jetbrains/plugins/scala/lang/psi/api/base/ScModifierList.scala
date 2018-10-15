package org.jetbrains.plugins.scala
package lang
package psi
package api
package base

import com.intellij.psi.tree.IElementType
import com.intellij.psi.{PsiAnnotation, PsiModifier, PsiModifierList}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes

/**
  * @author Alexander Podkhalyuzin
  *         Date: 22.02.2008
  */
trait ScModifierList extends ScalaPsiElement with PsiModifierList {

  //only one access modifier can occur in a particular modifier list
  def accessModifier: Option[ScAccessModifier]

  def modifiers: Set[String]

  def hasExplicitModifiers: Boolean

  final def isPrivate: Boolean = accessModifier.exists(_.isPrivate)

  final def isProtected: Boolean = accessModifier.exists(_.isProtected)

  override final def getApplicableAnnotations: Array[PsiAnnotation] =
    PsiAnnotation.EMPTY_ARRAY

  override final def addAnnotation(qualifiedName: String): PsiAnnotation = null

  override final def hasModifierProperty(name: String): Boolean = modifiers(name)

  override final def hasExplicitModifier(name: String): Boolean = false

  override final def checkSetModifierProperty(name: String, value: Boolean): Unit = {}
}

object ScModifierList {

  private[psi] object NonAccessModifier extends Enumeration {

    import PsiModifier.{ABSTRACT, FINAL}
    import ScalaTokenTypes._

    case class Val(keyword: String,
                   prop: IElementType) extends super.Val(keyword)

    val Final = Val(FINAL, kFINAL)

    val Abstract = Val(ABSTRACT, kABSTRACT)

    val Override = Val("override", kOVERRIDE)

    val Implicit = Val("implicit", kIMPLICIT)

    val Sealed = Val("sealed", kSEALED)

    val Lazy = Val("lazy", kLAZY)

    val Case = Val("case", kCASE)
  }

  implicit class ScModifierListExt(val list: ScModifierList) extends AnyVal {

    import NonAccessModifier._

    def isFinal: Boolean = hasModifier(Final)

    def isAbstract: Boolean = hasModifier(Abstract)

    def isOverride: Boolean = hasModifier(Override)

    def isImplicit: Boolean = hasModifier(Implicit)

    def isSealed: Boolean = hasModifier(Sealed)

    def isLazy: Boolean = hasModifier(Lazy)

    def isCase: Boolean = hasModifier(Case)

    private def hasModifier(value: Val) =
      list.hasModifierProperty(value.keyword)
  }

}