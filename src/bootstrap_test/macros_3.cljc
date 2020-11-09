(ns bootstrap-test.macros-3
  (:require [bootstrap-test.wrap-3 :as w3]))

(defmacro wrap-3 [x]
  `(w3/wrap ~x))
