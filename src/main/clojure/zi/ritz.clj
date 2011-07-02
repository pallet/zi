(ns zi.ritz
  "ritz mojo for zi plugin"
  (:require
   [zi.core :as core])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
    org.apache.maven.plugin.ContextEnabled
    org.apache.maven.plugin.Mojo
    org.apache.maven.plugin.MojoExecutionException))

(deftype
    ^{Goal "ritz"
      RequiresDependencyResolution "test"}
    RitzMojo
  [
   ^{Parameter
     {:expression "${basedir}" :required true :readonly true}}
   base-directory

   ^{Parameter
     {:defaultValue "${project.compileClasspathElements}"
      :required true :readonly true :description "Compile classpath"}}
   classpath-elements

   ^{Parameter
     {:defaultValue "${project.testClasspathElements}"
      :required true :readonly true}}
   test-classpath-elements

   ^{Parameter
     {:defaultValue "${project.build.outputDirectory}" :required true}}
   output-directory

   ^{Parameter {}}
   sourceDirectories

   ^{Parameter {}}
   replScript

   ^{Parameter
     {:expression "${clojure.swank.port}" :defaultValue "4005"}}
   ^Integer
   port

   ^{Parameter
     {:expression "${clojure.swank.encoding}" :defaultValue "iso-8859-1"}}
   ^String
   encoding

   ^{Parameter
     {:expression "${clojure.swank.host}" :defaultValue "localhost"}}
   ^String
   swank-host

   ^{:volatile-mutable true}
   log

   plugin-context
   ]

  Mojo
  (execute [_]
    (let [tmpfile (try
                    (File/createTempFile "swank" ".port")
                    (catch java.io.IOException e
                      (throw
                       (MojoExecutionException.
                        "Could not create swank port file" e))))]
      (core/eval-clojure
       (or sourceDirectories ["src/main/clojure" "src/test/clojure"])
       test-classpath-elements
       `(do
          (require '~'swank-clj.socket-server)
          (swank-clj.socket-server/start
           {:port-file ~(.getPath tmpfile)
            :host ~swank-host
            :port ~(Integer/parseInt port)
            :encoding ~encoding
            :dont-close true})))))

  (setLog [_ logger] (set! log logger))
  (getLog [_] log)

  ContextEnabled
  (setPluginContext [_ context] (reset! plugin-context context))
  (getPluginContext [_] @plugin-context))

(defn make-RitzMojo
  []
  (RitzMojo. nil nil nil nil nil nil nil nil nil nil (atom nil)))
