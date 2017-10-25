(ns test.a
  (:require-macros [test.b :refer [xyz]]))
(enable-console-print!)

(xyz :a :b :c)