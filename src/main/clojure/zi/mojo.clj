(ns zi.mojo
  "Mojo syntax sugar"
  (:import
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]))

(defn parameter
  "Define a mojo parameter field"
  [parameter-name parameter-doc & {:as parameter-meta}]
  (vary-meta
   parameter-name
   assoc 'clojure.maven.annotations.Parameter
   (assoc parameter-meta :description parameter-doc)))

(defmacro component
  "Define an injected field"
  [name doc & {:as component-meta}]
  (vary-meta name merge (assoc component-meta :description doc)))

(defmacro defmojo
  "Define a mojo with the given name.  This defines some common fields
   and leaves you to just specify a body for the execute function."
  [mojo-name mojo-meta [& fields] & body]
  `(do
     (deftype ~(vary-meta mojo-name merge mojo-meta)
         [~@fields
          ~(parameter 'base-directory
                      "Project base directory"
                      :expression "${basedir}"
                      :required true :readonly true)
          ~(parameter 'source-directory
                      "Source directory for clojure source"
                      :alias "sourceDirectory"
                      :defaultValue "${project.build.sourceDirectory}"
                      :required true)
          ~(parameter 'test-source-directory
                      "Source directory for clojure test source"
                      :alias "testSourceDirectory"
                      :defaultValue "${project.build.testSourceDirectory}"
                      :required true)
          ~(parameter 'classpath-elements
                      "Classpath elements"
                      :defaultValue "${project.compileClasspathElements}"
                      :required true :readonly true)
          ~(parameter 'test-classpath-elements
                      "Test classpath elements"
                      :defaultValue "${project.testClasspathElements}"
                      :required true :readonly true)
          ~(parameter 'compile-namespaces
                      "Which namespaces to compile"
                      :alias "compileNamespaces"
                      :typename "java.util.List")
          ~(parameter 'output-directory
                      "Project target directory"
                      :expression "${project.build.directory}"
                      :required true :readonly true)
          ~(vary-meta 'log assoc :volatile-mutable true)
          ~'plugin-context]

       org.apache.maven.plugin.Mojo
       (~'execute [_#]
         ~@body)
       (~'setLog [_# logger#] (set! ~'log logger#))
       (~'getLog [_#] ~'log)

       org.apache.maven.plugin.ContextEnabled
       (~'setPluginContext [_# context#] (reset! ~'plugin-context context#))
       (~'getPluginContext [_#] (deref ~'plugin-context)))
     (defn ~(symbol (str "make-" (name mojo-name))) []
       (new ~mojo-name ~@(repeat (+ (count fields) 8) nil) (atom nil)))))
