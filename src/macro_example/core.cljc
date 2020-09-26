(ns macro-example.core
  #?(:cljs (:require-macros macro-example.core)))

(defmacro current-ns-str []
  ;; simple macro that returns info only available at compile time
  (name (.-name *ns*)))



