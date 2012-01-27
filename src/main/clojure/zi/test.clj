(ns zi.test
  "Run tests"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [zi.log :as log]
   [classlojure.core :as classlojure]
   [clojure.string :as string])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException))

(defn find-tests
  "Find all clj files under the test source path, and return the
   namespaces."
  [source-paths]
  (mapcat core/find-namespaces source-paths))

(defn report-test
  [result]
  (case (:type result)
    :fail (log/error
            "FAIL %s expected: %s  actual: %s"
            (:message result "") (:expected result) (:actual result))
    :error (log/error
             "%s expected: %s  actual: %s"
             (:message result "") (:expected result) (:actual result))))

(defn run-tests
  [classpath-elements test-source-directory init-script]
  (let [cl (core/classloader-for classpath-elements)
        bindings (gensym "bindings")
        body (gensym "body")
        test-ns-list (find-tests
                      (core/clojure-source-paths test-source-directory))
        test-ns-symbols (map #(list `quote (symbol %)) test-ns-list)
        init-script (when init-script
                      (read-string (str "(do " init-script ")")))]
    (when (seq test-ns-symbols)
      (log/debug
       (str "Running tests for " (string/join ", " (map name test-ns-list))))
      (log/debug
       (str "Init script " init-script))
      (classlojure/eval-in
       cl
       `(do
          ~init-script
          (require 'clojure.main)
          (clojure.main/with-bindings
            (require '~'clojure.test)
            (defmacro ~'portable-redef [ [& ~bindings] & ~body ]
              (if (find-var 'clojure.core/with-redefs)
                `(clojure.core/with-redefs [~@~bindings] ~@~body)
                `(binding [~@~bindings] ~@~body))))))
      (let [results
            (classlojure/eval-in
             cl
             `(fn [out# err#]
                (clojure.main/with-bindings
                  (binding [*out* out# *err* err#]
                    (let [results# (atom [])
                          original-report# clojure.test/report
                          report# (fn [m#]
                                    (original-report# m#)
                                    (swap!
                                     results# conj
                                     (-> m#
                                         (update-in
                                          [:actual] #(when % (pr-str %)))
                                         (update-in
                                          [:ns] #(when %
                                                   (list
                                                    `quote (ns-name %)))))))]
                      (require ~@test-ns-symbols)
                      (~'portable-redef
                       [clojure.test/report report#]
                       (binding [clojure.test/*test-out* *out*]
                         (clojure.test/run-tests ~@test-ns-symbols)))
                      (map #(update-in % [:message] str) @results#)))))
             *out* *err*)
            passes (dec (count (map :pass results)))
            summary (last results)]
        (log/debug (pr-str results))
        (log/info
         (format
          "Tests run: %d, passed: %d, failed: %d, errors: %d"
          (:test summary) (:pass summary) (:fail summary) (:error summary)))
        (when (or (pos? (:fail summary)) (pos? (:error summary)))
          (throw
           (org.apache.maven.plugin.MojoFailureException. "Tests failed")))))))

(mojo/defmojo ClojureTest
  {Goal "test"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:expression "${clojure.test-ns}" :defaultValue ""
      :description "List of namespaces to run tests for"}}
   ^String
   namespaces

   ^{Parameter
     {:expression "${skipTests}"
      :description "Skip test execution"
      :alias "skipTests"
      :defaultValue "false"
      :typename "boolean"}}
   skip-tests]

  (when-not ({"true" true "false" false} skip-tests)
    (run-tests
     (concat
      (core/clojure-source-paths test-source-directory)
      (core/clojure-source-paths source-directory)
      test-classpath-elements)
     test-source-directory
     init-script)))
