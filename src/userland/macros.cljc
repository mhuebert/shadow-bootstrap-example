(ns userland.macros
  (:require [userland.macros-2 :as m2]

    ;; removing this ns causes `m2 is not defined` error
    ;        [userland.both :as b]

            )
  #?(:cljs (:require-macros userland.macros)))

(defmacro current-ns-str
  "simple macro that returns info only available at compile time"
  []
  ;; no-op is just to test re-use of macros from other namespaces
  `(m2/no-op
     ~(name (.-name *ns*))))

