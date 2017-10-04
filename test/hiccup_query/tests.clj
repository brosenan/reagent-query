(ns hiccup-query.tests
  (:require [devcards.core :refer [deftest]]))

(defmacro fact [& body]
  `(deftest ~@body))

