(ns bootstrap-test.reagent
  (:require [reagent.ratom :as ra])
  #?(:cljs (:require-macros bootstrap-test.reagent)))

(defmacro with-let [& body]
  `(ra/with-let ~@body))

