import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
class ParseToyJava {
 public static void main(String[] args) throws Exception {
  var compiler=ToolProvider.getSystemJavaCompiler();var diagnostics=new DiagnosticCollector<JavaFileObject>();
  try(var fm=compiler.getStandardFileManager(diagnostics,null,null)) {
   var task=(JavacTask)compiler.getTask(null,fm,diagnostics,List.of("-proc:none"),null,fm.getJavaFileObjects(args));
   int n=0;for(var tree:task.parse())n++;
   for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(d.toString());
   System.out.println("Parsed "+n+" Java source files without syntax errors (not type checking)");
  }
 }
}
