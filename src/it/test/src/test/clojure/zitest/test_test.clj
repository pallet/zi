(ns zitest.test-test
  (:require zitest.test)
  (:use clojure.test))

(deftest one
  (is (= 1 (+ 0 1)))
  (is true)
  ;; (is false)
  ;; (is (throw (Exception. "some execption")))
  )
