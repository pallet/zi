(ns zi.simple
  "Simple mojo for zi plugin to check injection"
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
    org.apache.maven.plugin.ContextEnabled
    org.apache.maven.plugin.Mojo
    org.apache.maven.plugin.MojoExecutionException))

(deftype
    ^{Goal "simple"
      RequiresDependencyResolution "test"}
    SimpleMojo
  [
   ^{Component {:role "org.sonatype.aether.RepositorySystem"}}
   repoSystem

   ^{:volatile-mutable true}
   log

   plugin-context
   ]

  Mojo
  (execute [_]
    (when-not repoSystem
      (throw (MojoExecutionException. "repoSystem was not injected"))))

  (setLog [_ logger] (set! log logger))
  (getLog [_] log)

  ContextEnabled
  (setPluginContext [_ context] (reset! plugin-context context))
  (getPluginContext [_] @plugin-context))

(defn make-SimpleMojo
  []
  (SimpleMojo. nil nil (atom nil)))
