(ns zi.test-test
  (:require
   [zi.test :as test])
  (:use
   clojure.test))

(def log (reify org.apache.maven.plugin.logging.Log
           (^void debug [_ ^CharSequence str])
           (^void debug [_ ^Throwable str])
           (^void info [_ ^CharSequence str])
           (^void info [_ ^Throwable str])
           (^void warn [_ ^CharSequence str])
           (^void warn [_ ^Throwable str])
           (^void error [_ ^CharSequence str])
           (^void error [_ ^Throwable str])
           (isDebugEnabled [_])
           (isErrorEnabled [_])
           (isInfoEnabled [_])
           (isWarnEnabled [_])))

(deftest run-tests-test
  (is (test/run-tests
       (->>
        (iterate #(.getParent %) (clojure.lang.RT/baseLoader))
        (take-while #(instance? java.net.URLClassLoader %))
        (mapcat #(.getURLs %))
        (map #(.getPath %)))
       "src/test/clojure"
       log)))
