(ns zitest.test-test
  (:require zitest.test)
  (:use clojure.test)
  (:import java.util.Date))

(deftest one
  (is (= 1 (+ 0 1)))
  (is true)
  ;; (is false)
  ;; (is (throw (Exception. "some execption")))
  )

;; this was crashing zi, due to the date being used as a message
(deftest crash-zi (is (= 42 ) (Date.)))
