(ns zi.core-test
  (:require
   [zi.core :as core])
  (:use
   clojure.test))

(deftest find-namespaces-test
  (is (every? string? (core/find-namespaces "src/main/clojure"))))

(deftest smallest-indent-test
  (is (= 2 (core/smallest-indent "  a\n\n  b\n")))
  (is (= 0 (core/smallest-indent ""))))

(deftest unindent-test
  (is (= "a\n\nb" (core/unindent "  a\n\n  b\n")))
  (is (nil? (core/unindent nil))))
