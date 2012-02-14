(ns zi.test
  "Run tests"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
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
  [source-paths exclude-ns-regex]
  (let [nss (mapcat core/find-namespaces source-paths)]
    (if-not (string/blank? exclude-ns-regex)
      (remove #(re-matches (re-pattern exclude-ns-regex) %) nss)
      nss)))

(defn report-test
  [log result]
  (case (:type result)
    :fail (.error log
                  "FAIL %s expected: %s  actual: %s"
                  (:message result "") (:expected result) (:actual result))
    :error (.error log "%s expected: %s  actual: %s"
                   (:message result "") (:expected result) (:actual result))))

(defn run-tests
  [classpath-elements test-source-directory log init-script exclude-ns-regex]
  (let [cl (core/classloader-for classpath-elements)
        bindings (gensym "bindings")
        body (gensym "body")
        test-ns-list (find-tests
                      (core/clojure-source-paths test-source-directory)
                      exclude-ns-regex)
        test-ns-symbols (map #(list `quote (symbol %)) test-ns-list)
        init-script (when init-script
                      (read-string (str "(do " init-script ")")))]
    (when (seq test-ns-symbols)
      (.debug
       log
       (str "Running tests for " (string/join ", " (map name test-ns-list))))
      (.debug
       log
       (str "Init script " init-script))
      (classlojure/eval-in
       cl
       `(do
          ~init-script
          (require 'clojure.main)
          (clojure.main/with-bindings
            (require '~'clojure.test)
            (defmacro ~'portable-redef [ [& ~bindings] & ~body ]
              (if (ns-resolve 'clojure.core 'with-redefs)
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
                                     (->
                                      m#
                                      (update-in
                                       [:actual]
                                       #(when %
                                          (try
                                            (pr-str %)
                                            (catch Exception _#
                                              "Unable to show actual value"))))
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
        (.debug
         log (let [res (pr-str results)
                   l (count res)]
               (str
                "RESULTS: "
                (subs res 0 (min 1024 l))
                (if (> l 1024) "..." ""))))
        (.info
         log
         (format
          "Tests run: %d, passed: %d, failed: %d, errors: %d"
          (:test summary) (:pass summary) (:fail summary) (:error summary)))
        (when (or (pos? (:fail summary)) (pos? (:error summary)))
          (throw
           (org.apache.maven.plugin.MojoFailureException. "Tests failed")))))))

(mojo/defmojo ClojureTest
  {Goal "test"
   Phase "test"
   RequiresDependencyResolution "test"}
  [;; list parameters not yet functional in zi
   ^{Parameter
     {:expression "${clojure.test-ns}" :defaultValue ""
      :description "List of namespaces to run tests for"}}
   ^String
   namespaces
   ^{Parameter
     {:expression "${clojure.test.exclude-ns}"
      :description "Regular expression for excluding namespaces from testing"
      :alias "excludeTestNamespacesMatching"
      :typename "java.lang.String"}}
   ^String
   exclude-test-ns-regex

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
     log
     init-script
     exclude-test-ns-regex)))
