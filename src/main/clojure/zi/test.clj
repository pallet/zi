(ns zi.test
  "Run tests"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core])
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
  [log result]
  (case (:type result)
    :fail (.error log
                  "FAIL %s expected: %s  actual: %s"
                  (:message result "") (:expected result) (:actual result))
    :error (.error log "%s expected: %s  actual: %s"
                   (:message result "") (:expected result) (:actual result))))

(defn run-tests
  [classpath-elements test-source-directory log]
  (let [cl (core/classloader-for classpath-elements)
        bindings (gensym "bindings")
        body (gensym "body")
        test-ns-list (find-tests
                      (core/clojure-source-paths test-source-directory))
        test-ns-symbols (map #(list `quote (symbol %)) test-ns-list)]
    (classlojure/eval-in
     cl
     `(do
        (require '~'clojure.test)
        (defmacro ~'redef [ [& ~bindings] & ~body ]
          (if (find-var 'clojure.core/with-redefs)
            `(with-redefs [~@~bindings] ~@~body)
            `(binding [~@~bindings] ~@~body)))))
    (let [results (classlojure/eval-in
                   cl
                   `(let [results# (atom [])
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
                                                 (list `quote (ns-name %)))))))]
                      (~'redef
                       [clojure.test/report report#]
                       (binding [clojure.test/*test-out* *out*]
                         (require ~@test-ns-symbols)
                         (clojure.test/run-tests ~@test-ns-symbols)))
                      @results#))
          passes (dec (count (map :pass results)))
          summary (last results)]
      (.info
       log
       (format
        "Tests run: %d, passed: %d, failed: %d, errors: %d"
        (:test summary) (:pass summary) (:fail summary) (:error summary))))))

(mojo/defmojo ClojureTest
  {Goal "test"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:expression "${clojure.test-ns}" :defaultValue ""
      :description "List of namespaces to run tests for"}}
   ^String
   namespaces]

  (run-tests
   (concat
    (core/clojure-source-paths test-source-directory)
    (core/clojure-source-paths source-directory)
    test-classpath-elements)
   test-source-directory log))
