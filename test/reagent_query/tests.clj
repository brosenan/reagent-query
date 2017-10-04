(ns reagent-query.tests
  (:require [devcards.core :refer [deftest]]))

(defmacro fact [& body]
  `(deftest ~@body))

