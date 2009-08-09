package org.jetbrains.plugins.scala
package lang
package psi
package api
package base
package types

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import toplevel.ScNamedElement

/** 
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
*/

trait ScSelfTypeElement extends ScNamedElement {
  def typeElement = findChild(classOf[ScTypeElement])

}