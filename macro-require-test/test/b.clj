(ns test.b
    (:require [test.c :refer [join-special]]))

(defmacro xyz [& body]
          `(prn ~(join-special body)))