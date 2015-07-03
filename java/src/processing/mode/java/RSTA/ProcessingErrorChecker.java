package processing.mode.java.RSTA;

import java.net.URL;

import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ExtendedHyperlinkListener;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;

import processing.app.Base;
import processing.app.EditorStatus;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ErrorCheckerService;
import processing.mode.java.pdex.ErrorMarker;
import processing.mode.java.pdex.Problem;


public class ProcessingErrorChecker extends ErrorCheckerService implements Parser{
  
  private DefaultParseResult result;

  public ProcessingErrorChecker(JavaEditor debugEditor) {
    super(debugEditor);
    result = new DefaultParseResult(this);
  }

  @Override
  public ExtendedHyperlinkListener getHyperlinkListener() {
    return null;
  }

  @Override
  public URL getImageBase() {
    return null;
  }

  /**
   * Overriden to always be true. Mwahahaha... 
   * On a more serious note, TODO: permit user to
   * disable syntax checking via this method
   */
  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public ParseResult parse(RSyntaxDocument doc, String style) {
    result.clearNotices();
    Element root = doc.getDefaultRootElement();
    int lineCount = root.getElementCount();
    result.setParsedLines(0, lineCount-1);
    checkCode();    
    // TODO: Something likely needs to be done in there:
    checkForMissingImports();
    System.out.println("Parsed");
    for (Problem p: problemsList) {
      if (p.getTabIndex() == editor.getSketch().getCurrentCodeIndex()) {
        result.addNotice(
          new DefaultParserNotice(this,
                                  p.getMessage(),
                                  p.getLineNumber(),
                                  p.getPDEStartOffset() +
                                    p.getPDELineStartOffset(),
                                  p.getPDELineStopOffset() -
                                    p.getPDELineStartOffset() +
                                    1));
      }
      /*
      System.out.println("Msg: " + p.getMessage() + " Line: " + p.getLineNumber()  + 
                         " Start:" + (p.getPDEStartOffset() +
                         p.getPDELineStartOffset()) + " Len:" + 
                         (p.getPDEStartOffset() + 
                            p.getPDELineStopOffset() -
                            p.getPDELineStartOffset()));
      System.out.println("Start Offset:" + p.getPDEStartOffset() +
                         "\nLine start offset:" + p.getPDELineStartOffset() +  
                         "\nStop Offset:" + p.getPDEStopOffset() +
                         "\nLine stop offset:" + p.getPDELineStopOffset());
                         */
    }
    return result;
  }
  
  // TODO: Should this maybe use the RSyntaxDocument instead?
  protected boolean checkCode() {
    try {
      sourceCode = preprocessCode(editor.getSketch().getMainProgram());
      compilationUnitState = 0;
      syntaxCheck();
      // No syntax errors, proceed for compilation check, Stage 2.

      if (!hasSyntaxErrors()) {

      }
      if (problems.length == 0 && !editor.hasJavaTabs()) {
        sourceCode = xqpreproc.doYourThing(sourceCode, programImports);
        prepareCompilerClasspath();
        compileCheck();
      }

      astGenerator.buildAST(cu);
      if (!JavaMode.errorCheckEnabled) {
        problemsList.clear();
        Base.log("Error Check disabled, so not updating UI.");
      }
      calcPDEOffsetsForProbList();
      updateErrorTable();
      
      updateEditorStatus();
      
      editor.updateErrorToggle();
      return true;

    } catch (Exception e) {
      Base.log("Oops! [ErrorCheckerService.checkCode]: " + e);
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public void updateEditorStatus() {
    if (editor.getStatusMode() == EditorStatus.EDIT) return;

    // This line isn't an error line anymore, so probably just clear it
    if (editor.statusMessageType == JavaEditor.STATUS_COMPILER_ERR) {
      editor.statusEmpty();
      return;
    }
  }
}