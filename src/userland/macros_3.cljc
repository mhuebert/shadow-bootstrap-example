(ns userland.macros-3
  #?(:cljs (:require-macros userland.macros-3)))

(defmacro no-op [expr]
  expr)
