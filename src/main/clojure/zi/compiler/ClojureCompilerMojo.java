package zi.compiler;

import org.apache.maven.project.MavenProject;

import org.apache.maven.plugin.CompilerMojo;
import org.apache.maven.plugin.AbstractCompilerMojo;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;

import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.lang.reflect.Field;

 // * execute phase "compile"
 // * phase compile

/**
 * Compiles application sources
 *
 * @author <a href="mailto:hugo@hugoduncan.org">Hugo Duncan</a>
 * @extendsPlugin maven-compiler-plugin
 * @goal compile
 * @threadSafe
 * @requiresDependencyResolution compile
 */
public class ClojureCompilerMojo
  extends CompilerMojo
{
  /**
   * The compiler id of the compiler to use. See this
   * <a href="non-javac-compilers.html">guide</a> for more information.
   *
   * @parameter expression="${maven.compiler.compilerId}" default-value="clojure"
   */
  private String compilerId;

  /**
   * The project.
   *
   * @parameter expression="${project}"
   * @required
   */
  private MavenProject project;

  protected void forwardCompilerId()
  {
    try {
      Field f = AbstractCompilerMojo.class.getDeclaredField("compilerId");
      f.setAccessible(true);
      f.set(this,compilerId);
    }
    catch (NoSuchFieldException e)
    {
      getLog().warn("failed to forward compilerId (no such field)");
    }
    catch (IllegalAccessException e)
    {
      getLog().warn("failed to forward compilerId (illegal access)");
    }
  }

  protected Object getField(Class klass, String fieldName)
  {
    try {
      Field f = klass.getDeclaredField(fieldName);
      f.setAccessible(true);
      return f.get(this);
    }
    catch (NoSuchFieldException e)
    {
      getLog().warn("failed to get field " + fieldName + " (no such field)");
    }
    catch (IllegalAccessException e)
    {
      getLog().warn("failed to get field " + fieldName + " (illegal access)");
    }
    return null;
  }

  // This should use project.addCompileSourceRoot to work
  protected void addClojureCompileSourceRoots()
  {
    List<String> sourceRoots = getCompileSourceRoots();
    List<String> newRoots = new java.util.LinkedList();
    for (String srcDir : sourceRoots)
    {
      if (srcDir.endsWith("/java") || srcDir.endsWith("\\java"))
      {
        newRoots.add(srcDir.substring(0, srcDir.length()-4) + "clojure");
      }
    }
    for (String srcDir : newRoots)
    {
      project.addCompileSourceRoot(srcDir);
      sourceRoots.add(srcDir);
    }
  }

  protected SourceInclusionScanner getSourceInclusionScanner( int staleMillis )
  {
    SourceInclusionScanner scanner = null;
    Set<String> includes = (Set<String>)getField(CompilerMojo.class, "includes");
    Set<String> excludes = (Set<String>)getField(CompilerMojo.class, "excludes");

    if ( includes.isEmpty() && excludes.isEmpty() )
    {
      scanner = new StaleSourceScanner( staleMillis );
    }
    else
    {
      if ( includes.isEmpty() )
      {
        includes.add( "**/*.clj" );
      }
      scanner = new StaleSourceScanner( staleMillis, includes, excludes );
    }

    return scanner;
  }

  protected SourceInclusionScanner getSourceInclusionScanner( String inputFileEnding )
  {
    SourceInclusionScanner scanner = null;
    Set<String> includes = (Set<String>)getField(CompilerMojo.class, "includes");
    Set<String> excludes = (Set<String>)getField(CompilerMojo.class, "excludes");

    if ( includes.isEmpty() && excludes.isEmpty() )
    {
      includes = Collections.singleton( "**/*." + inputFileEnding );
      scanner = new SimpleSourceInclusionScanner( includes, Collections.EMPTY_SET );
    }
    else
    {
      if ( includes.isEmpty() )
      {
        includes.add( "**/*." + inputFileEnding );
      }
      scanner = new SimpleSourceInclusionScanner( includes, excludes );
    }

    return scanner;
  }

  public void execute()
    throws MojoExecutionException, CompilationFailureException
  {
    forwardCompilerId();
    addClojureCompileSourceRoots();
    super.execute();
  }
}

