(ns userland.both
  (:require-macros [userland.both :as b]))

(defn hello [x]
  (b/wrap x))
