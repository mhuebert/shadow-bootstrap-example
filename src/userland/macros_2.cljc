(ns userland.macros-2
  ;; if I remove this :require-macros self-require,
  ;; then other macro namespaces can't use macros in this namespace,
  ;; no matter how they require it
  #?(:clj (:require [userland.macros-3 :as m3]))
  #?(:cljs (:require-macros userland.macros-2
                            [userland.macros-3 :as m3])))

(defmacro no-op [expr]
  `(do
     (prn '`m3/no-op)
     ~expr #_(m3/no-op ~expr)))

