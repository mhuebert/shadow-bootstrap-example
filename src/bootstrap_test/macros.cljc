(ns bootstrap-test.macros
  (:require [bootstrap-test.sub-macros :as sub]))

(defmacro wrap [x]
  `(sub/wrap ~x))
