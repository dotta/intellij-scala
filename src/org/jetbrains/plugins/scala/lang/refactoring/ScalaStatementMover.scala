package org.jetbrains.plugins.scala
package lang.refactoring

import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover.MoveInfo
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.editorActions.moveUpDown.{LineRange, LineMover}
import lang.psi.api.toplevel.typedef.ScMember
import lang.psi.ScalaPsiElement
import lang.psi.api.base.patterns.ScCaseClause
import com.intellij.psi.{PsiComment, PsiWhiteSpace, PsiElement, PsiFile}
import org.jetbrains.plugins.scala.extensions._
import lang.psi.api.toplevel.imports.ScImportStmt
import lang.psi.api.expr._

/**
 * Pavel Fatin
 */

class ScalaStatementMover extends LineMover {
  override def checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean = {
    if(!super.checkAvailable(editor, file, info, down)) return false
    if(editor.getSelectionModel.hasSelection) return false
    if(!file.isInstanceOf[ScalaFile]) return false

    val canBeTarget: PsiElement => Boolean = {
      case _: ScExpression => true
      case _: ScMember => true
      case _: ScCaseClause => true
      case _: ScImportStmt => true
      case _ => false
    }

    def aim[T <: ScalaPsiElement](cl: Class[T]): Option[(T, PsiElement)] = {
      findElementAt(cl, editor, file, info.toMove.startLine).flatMap { source =>
        val siblings = if(down) source.nextSiblings else source.prevSiblings
        siblings.filter(!_.isInstanceOf[PsiComment] )
                .takeWhile(it => it.isInstanceOf[PsiWhiteSpace] || canBeTarget(it))
                .find(canBeTarget)
                .map(target => (source, target))
      }
    }

    val pair = aim(classOf[ScCaseClause])
            .orElse(aim(classOf[ScMember]))
            .orElse(aim(classOf[ScImportStmt]))
            .orElse(aim(classOf[ScIfStmt]))
            .orElse(aim(classOf[ScForStatement]))
            .orElse(aim(classOf[ScMethodCall]).filter(p => isControlStructureLikeCall(p._1)))

    pair.foreach { it =>
      info.toMove = rangeOf(it._1, editor)
      info.toMove2 = rangeOf(it._2, editor)
    }

    pair.isDefined
  }

  private def isControlStructureLikeCall(call: ScMethodCall): Boolean = {
    call.argumentExpressions.lastOption.exists { it =>
      it.isInstanceOf[ScBlockExpr] && it.getText.contains('\n')
    }
  }

  private def rangeOf(e: PsiElement, editor: Editor) = {
    val begin = editor.offsetToLogicalPosition(e.getTextRange.getStartOffset).line
    val end = editor.offsetToLogicalPosition(e.getTextRange.getEndOffset).line + 1
    new LineRange(begin, end)
  }

  private def findElementAt[T <: ScalaPsiElement](cl: Class[T], editor: Editor, file: PsiFile, line: Int): Option[T] = {
    val edges = edgeLeafsOf(line, editor, file)

    val left = edges._1.flatMap(PsiTreeUtil.getParentOfType(_, cl, false).toOption)
    val right = edges._2.flatMap(PsiTreeUtil.getParentOfType(_, cl, false).toOption)

    left.zip(right)
            .filter(p => p._1 == p._2 || p._1.parentsInFile.contains(p._2))
            .map(_._2)
            .filter(it => editor.offsetToLogicalPosition(it.getTextOffset).line == line)
            .headOption
  }

  private def edgeLeafsOf(line: Int, editor: Editor, file: PsiFile): (Option[PsiElement], Option[PsiElement]) = {
    val document = editor.getDocument

    val start = document.getLineStartOffset(line)
    val end = start.max(document.getLineEndOffset(line) - 1)

    val span = start.to(end)

    def firstLeafOf(seq: Seq[Int]) = seq.view.flatMap(file.getNode.findLeafElementAt(_).toOption.toSeq)
            .filter(!_.getPsi.isInstanceOf[PsiWhiteSpace]).map(_.getPsi).headOption

    (firstLeafOf(span), firstLeafOf(span.reverse))
  }
}