(ns userland.macros-2
  ;; if I remove this :require-macros self-require,
  ;; then other macro namespaces can't use macros in this namespace,
  ;; no matter how they require it
  #?(:cljs (:require-macros userland.macros-2))
  )

(defmacro no-op [expr]
  expr)

