(ns bootstrap-test.macros-3
  (:require [bootstrap-test.wrap-3 :as wrap-3]))

(defmacro wrap-3 [x]
  `(wrap-3/wrap ~x))
