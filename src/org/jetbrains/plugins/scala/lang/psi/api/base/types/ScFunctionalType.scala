package org.jetbrains.plugins.scala
package lang
package psi
package api
package base
package types

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/** 
* @author Alexander Podkhalyuzin
* Date: 22.02.2008
*/

trait ScFunctionalTypeElement extends ScTypeElement {
  def paramTypeElement = findChildByClass(classOf[ScTypeElement])

  def returnTypeElement = findChildrenByClass(classOf[ScTypeElement]) match {
    case Array(single) => None
    case many => Some(many(1))
  }
}