(ns zi.core-test
  (:require
   [zi.core :as core])
  (:use
   clojure.test))

(deftest find-namespaces-test
  (is (every? string? (core/find-namespaces "src/main/clojure"))))
